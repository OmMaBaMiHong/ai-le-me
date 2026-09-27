package org.aileme.shejiao.admin.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import org.springframework.web.multipart.MultipartFile;
import org.aileme.shejiao.admin.dao.HongniangDao;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.api.service.HongniangService;
import org.aileme.shejiao.api.service.HongniangMatchCaseService;
import org.aileme.shejiao.api.service.HongniangUserRelationService;
import org.aileme.shejiao.api.service.XiangqinActivityService;
import org.aileme.shejiao.api.service.XiangqinEnrollmentService;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.Query;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangInfoEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangUserRelationEntity;
import org.aileme.shejiao.domain.entity.admin.XiangqinActivityEntity;
import org.aileme.shejiao.domain.entity.admin.XiangqinEnrollmentEntity;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 红娘Service实现
 *
 * @author system
 * @date 2026-01-27
 */
@Slf4j
@DS("master")
@Service("hongniangService")
public class HongniangServiceImpl extends ServiceImpl<HongniangDao, HongniangInfoEntity> implements HongniangService {

    @Value("${shejiao.file-upload.path:/upload}")
    private String fileUploadPath;

    @Value("${shejiao.file-upload.url-prefix:http://localhost:8080}")
    private String fileUploadUrlPrefix;

    @Value("${shejiao.hongniang-import.python-bin:}")
    private String hongniangImportPythonBin;

    @Value("${shejiao.hongniang-import.script-path:}")
    private String hongniangImportScriptPath;

    @Value("${spring.datasource.dynamic.datasource.master.url:}")
    private String masterJdbcUrl;

    @Value("${spring.datasource.dynamic.datasource.master.username:root}")
    private String masterDbUsername;

    @Value("${spring.datasource.dynamic.datasource.master.password:123456}")
    private String masterDbPassword;

    @Autowired
    private HongniangDao hongniangDao;

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private HongniangUserRelationService relationService;

    @Autowired
    private XiangqinActivityService xiangqinActivityService;

    @Autowired
    private XiangqinEnrollmentService xiangqinEnrollmentService;

    @Autowired
    private HongniangMatchCaseService hongniangMatchCaseService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String hongniangName = (String) params.get("hongniangName");
        String phone = (String) params.get("phone");
        Integer status = params.get("status") != null ? Integer.valueOf(params.get("status").toString()) : null;

        QueryWrapper<HongniangInfoEntity> wrapper = new QueryWrapper<>();
        if (hongniangName != null && !hongniangName.isEmpty()) {
            wrapper.like("hongniang_name", hongniangName);
        }
        if (phone != null && !phone.isEmpty()) {
            wrapper.like("phone", phone);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("create_time");

        IPage<HongniangInfoEntity> page = this.page(new Query<HongniangInfoEntity>().getPage(params), wrapper);
        return new PageUtils(page);
    }

    @Override
    @DSTransactional
    public void saveHongniang(HongniangInfoEntity hongniang) {
        // 检查手机号是否重复
        HongniangInfoEntity existing = hongniangDao.getByPhone(hongniang.getPhone());
        if (existing != null) {
            throw new LinfengException("手机号已存在");
        }

        // 初始化统计数据
        hongniang.setTotalUsers(0);
        hongniang.setTotalActivities(0);
        hongniang.setSuccessCount(0);
        hongniang.setCreateTime(new Date());
        hongniang.setUpdateTime(new Date());

        this.save(hongniang);
    }

    @Override
    @DSTransactional
    public void updateHongniang(HongniangInfoEntity hongniang) {
        // 检查手机号是否与其他红娘冲突
        HongniangInfoEntity existing = hongniangDao.getByPhone(hongniang.getPhone());
        if (existing != null && !existing.getId().equals(hongniang.getId())) {
            throw new LinfengException("手机号已被其他红娘使用");
        }

        hongniang.setUpdateTime(new Date());
        this.updateById(hongniang);
    }

    @Override
    public HongniangInfoEntity getByPhone(String phone) {
        return hongniangDao.getByPhone(phone);
    }

    @Override
    public HongniangInfoEntity getByUserId(Integer userId) {
        if (userId == null) {
            return null;
        }
        HongniangInfoEntity hongniang = this.getOne(new QueryWrapper<HongniangInfoEntity>()
                .eq("user_id", userId)
                .last("limit 1"), false);
        if (hongniang != null) {
            return hongniang;
        }
        AppUserEntity user = appUserService.getById(userId);
        if (user != null && user.getHongniangId() != null) {
            return this.getById(user.getHongniangId());
        }
        return null;
    }

    @Override
    @DSTransactional
    public int importUsersFromFile(MultipartFile file, Integer hongniangId) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new LinfengException("请选择要导入的文件");
        }
        if (hongniangId == null || this.getById(hongniangId) == null) {
            throw new LinfengException("红娘不存在或已删除");
        }
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new LinfengException("文件名为空");
        }
        String lowerFilename = filename.toLowerCase();
        if (lowerFilename.endsWith(".pdf")) {
            int count = importUsersByPython(file, hongniangId);
            updateStatistics(hongniangId);
            return count;
        }
        if (lowerFilename.endsWith(".docx")) {
            int count = legacyImportUsersFromStructuredFile(file, hongniangId, filename);
            updateStatistics(hongniangId);
            return count;
        }
        throw new LinfengException("仅支持PDF和DOCX格式文件");
    }

    private int legacyImportUsersFromStructuredFile(MultipartFile file, Integer hongniangId, String filename) throws Exception {
        String content;
        List<byte[]> images = new ArrayList<>();

        if (filename.toLowerCase().endsWith(".docx")) {
            content = parseDocx(file.getInputStream());
            // 提取 DOCX 中的图片
            images = extractImagesFromDocx(file.getInputStream());
        } else {
            throw new LinfengException("仅支持PDF和DOCX格式文件");
        }

        // 解析用户数据
        List<UserData> userDataList = parseUserData(content);

        // 为用户分配图片（简单的一对一分配）
//        for (int i = 0; i < userDataList.size() && i < images.size(); i++) {
//            try {
//                String avatarUrl = saveImage(images.get(i), "avatar_" + System.currentTimeMillis() + "_" + i);
//                userDataList.get(i).avatarUrl = avatarUrl;
//            } catch (Exception e) {
//                log.error("保存图片失败", e);
//            }
//        }

        int successCount = 0;
        List<HongniangUserRelationEntity> relations = new ArrayList<>();

        for (UserData userData : userDataList) {
            try {
                // 检查用户是否已存在（通过手机号）
                AppUserEntity existingUser = appUserService.lambdaQuery()
                        .eq(AppUserEntity::getMobile, userData.phone)
                        .one();

                Integer userId;
                if (existingUser != null) {
                    // 更新现有用户信息
                    userId = existingUser.getUid();
                    if (userData.avatarUrl != null) {
                        existingUser.setAvatar(userData.avatarUrl);
                    }
                    if (userData.age != null) {
                        existingUser.setAge(userData.age);
                    }
                    existingUser.setGender(userData.gender);
                    existingUser.setUpdateTime(new Date());
                    appUserService.updateById(existingUser);
                } else {
                    // 创建新用户
                    AppUserEntity newUser = new AppUserEntity();
                    newUser.setUsername(userData.name != null ? userData.name : "用户" + userData.phone.substring(7));
                    newUser.setMobile(userData.phone);
                    newUser.setGender(userData.gender);
                    newUser.setAge(userData.age);
                    newUser.setAvatar(userData.avatarUrl);

                    // 填充更多字段
                    if (userData.height != null) {
                        newUser.setHeight(userData.height);
                    }

                    // 学历映射：小学1，初中2，高中3，中专4，大专5，本科6，硕士7，博士8
                    if (userData.education != null) {
                        if (userData.education.contains("博士")) {
                            newUser.setEducation(8);
                        } else if (userData.education.contains("硕士")) {
                            newUser.setEducation(7);
                        } else if (userData.education.contains("本科")) {
                            newUser.setEducation(6);
                        } else if (userData.education.contains("大专")) {
                            newUser.setEducation(5);
                        } else if (userData.education.contains("中专")) {
                            newUser.setEducation(4);
                        } else if (userData.education.contains("高中")) {
                            newUser.setEducation(3);
                        } else if (userData.education.contains("初中")) {
                            newUser.setEducation(2);
                        } else if (userData.education.contains("小学")) {
                            newUser.setEducation(1);
                        }
                    }

                    // 婚姻状态：0未婚，1离异，2丧偶
                    if (userData.maritalStatus != null) {
                        if (userData.maritalStatus.contains("离异") || userData.maritalStatus.contains("离婚")) {
                            newUser.setMarryStatus(1);
                        } else if (userData.maritalStatus.contains("丧偶")) {
                            newUser.setMarryStatus(2);
                        } else if (userData.maritalStatus.contains("未婚")) {
                            newUser.setMarryStatus(0);
                        }
                    }

                    // 职业
                    if (userData.workUnit != null) {
                        newUser.setJob(userData.workUnit);
                    }

                    // 居住地
                    if (userData.residence != null) {
                        newUser.setAbodeCity(userData.residence);
                    }

                    // 籍贯
                    if (userData.nativePlace != null) {
                        newUser.setHomeCity(userData.nativePlace);
                    }

                    // 自我介绍和择偶要求放入info字段（JSON格式）
                    StringBuilder info = new StringBuilder();
                    if (userData.familyInfo != null) {
                        info.append("家庭情况：").append(userData.familyInfo).append("\n");
                    }
                    if (userData.houseCarStatus != null) {
                        info.append("房车情况：").append(userData.houseCarStatus).append("\n");
                    }
                    if (userData.healthStatus != null) {
                        info.append("健康状况：").append(userData.healthStatus).append("\n");
                    }
                    if (userData.salary != null) {
                        info.append("月薪：").append(userData.salary).append("\n");
                    }
                    if (info.length() > 0) {
                        newUser.setInfo(info.toString());
                    }

                    // 择偶要求
                    if (userData.mateRequirement != null) {
                        newUser.setAdminreHerart(userData.mateRequirement);
                    }

                    newUser.setStatus(0);
                    newUser.setCreateTime(new Date());

                    appUserService.save(newUser);
                    userId = newUser.getUid();
                }

                // 创建红娘-用户关联
                HongniangUserRelationEntity relation = new HongniangUserRelationEntity();
                relation.setHongniangId(hongniangId);
                relation.setUserId(userId);
                relation.setSourceType(1); // 红娘导入
                relation.setImportFileName(filename);
                relation.setCreateTime(new Date());
                relations.add(relation);

                successCount++;
            } catch (Exception e) {
                // 记录失败但继续处理其他用户
                log.error("导入用户失败: " + userData.name, e);
            }
        }

        // 批量插入关联关系
        if (!relations.isEmpty()) {
            relationService.batchInsert(relations);
        }

        return successCount;
    }

    private int importUsersByPython(MultipartFile file, Integer hongniangId) throws Exception {
        Path projectRoot = resolveProjectRoot();
        Path scriptPath = resolvePythonImportScript(projectRoot);
        String pythonBin = resolvePythonBin(projectRoot);
        DatabaseConnectionInfo databaseInfo = parseDatabaseConnectionInfo(masterJdbcUrl);

        String originalFilename = file.getOriginalFilename();
        String suffix = ".pdf";
        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }

        Path importFile = Files.createTempFile("hongniang-import-", suffix);
        Path previewFile = Files.createTempFile("hongniang-import-preview-", ".json");
        try {
            file.transferTo(importFile);
            List<String> command = new ArrayList<>();
            command.add(pythonBin);
            command.add(scriptPath.toString());
            command.add("--pdf");
            command.add(importFile.toString());
            command.add("--hongniang-id");
            command.add(String.valueOf(hongniangId));
            command.add("--limit");
            command.add("0");
            command.add("--machine-json");
            command.add("--preview-json");
            command.add(previewFile.toString());
            if (databaseInfo != null) {
                command.add("--db-host");
                command.add(databaseInfo.host());
                command.add("--db-port");
                command.add(String.valueOf(databaseInfo.port()));
                command.add("--db-name");
                command.add(databaseInfo.database());
                command.add("--db-user");
                command.add(masterDbUsername);
                command.add("--db-password");
                command.add(masterDbPassword);
            }

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(projectRoot.toFile());
            processBuilder.environment().put("PYTHONIOENCODING", StandardCharsets.UTF_8.name());
            Process process = processBuilder.start();

            int exitCode = process.waitFor();
            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8).trim();

            if (exitCode != 0) {
                String message = !stderr.isEmpty() ? stderr : stdout;
                throw new LinfengException("Python导入失败: " + abbreviate(message));
            }
            if (stdout.isEmpty()) {
                throw new LinfengException("Python导入未返回结果");
            }

            JSONObject result = JSON.parseObject(stdout);
            Integer count = result.getInteger("count");
            if (count == null) {
                throw new LinfengException("Python导入结果缺少 count 字段: " + abbreviate(stdout));
            }
            return count;
        } finally {
            Files.deleteIfExists(importFile);
            Files.deleteIfExists(previewFile);
        }
    }

    private Path resolveProjectRoot() {
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("scripts/hongniang_pdf_import.py"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new LinfengException("未找到项目根目录下的 scripts/hongniang_pdf_import.py");
    }

    private Path resolvePythonImportScript(Path projectRoot) {
        Path configured = normalizePath(projectRoot, hongniangImportScriptPath);
        if (configured != null) {
            if (!Files.exists(configured)) {
                throw new LinfengException("导入脚本不存在: " + configured);
            }
            return configured;
        }
        Path defaultScript = projectRoot.resolve("scripts/hongniang_pdf_import.py");
        if (!Files.exists(defaultScript)) {
            throw new LinfengException("导入脚本不存在: " + defaultScript);
        }
        return defaultScript;
    }

    private String resolvePythonBin(Path projectRoot) {
        Path configured = normalizePath(projectRoot, hongniangImportPythonBin);
        if (configured != null) {
            if (!Files.exists(configured)) {
                throw new LinfengException("Python解释器不存在: " + configured);
            }
            return configured.toString();
        }
        Path venvPython = projectRoot.resolve(".venv-hongniang-import/bin/python");
        if (Files.exists(venvPython)) {
            return venvPython.toString();
        }
        return "python3";
    }

    private Path normalizePath(Path projectRoot, String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return null;
        }
        Path path = Paths.get(candidate);
        return path.isAbsolute() ? path.normalize() : projectRoot.resolve(path).normalize();
    }

    private DatabaseConnectionInfo parseDatabaseConnectionInfo(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return null;
        }
        Matcher matcher = Pattern.compile("jdbc:mysql://([^:/?]+)(?::(\\d+))?/([^?]+)").matcher(jdbcUrl);
        if (!matcher.find()) {
            log.warn("无法解析主库 JDBC URL，将使用脚本默认数据库配置: {}", jdbcUrl);
            return null;
        }
        String host = matcher.group(1);
        int port = matcher.group(2) == null ? 3306 : Integer.parseInt(matcher.group(2));
        String database = matcher.group(3);
        return new DatabaseConnectionInfo(host, port, database);
    }

    private String abbreviate(String message) {
        if (message == null) {
            return "";
        }
        String normalized = message.replace('\n', ' ').replace('\r', ' ').trim();
        return normalized.length() <= 300 ? normalized : normalized.substring(0, 300) + "...";
    }

    @Override
    @DSTransactional
    public void updateStatistics(Integer hongniangId) {
        Map<String, Object> stats = calculateHongniangStats(hongniangId);
        int totalUsers = ((Number) stats.getOrDefault("totalUsers", 0)).intValue();
        int totalActivities = ((Number) stats.getOrDefault("totalActivities", 0)).intValue();
        int successCount = ((Number) stats.getOrDefault("successCount", 0)).intValue();
        hongniangDao.updateStatistics(hongniangId, totalUsers, totalActivities, successCount);
    }

    /**
     * 解析PDF文件
     */
    private String parsePdf(InputStream inputStream) throws Exception {
        PDDocument document = PDDocument.load(inputStream);
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);
        document.close();
        return text;
    }

    /**
     * 解析DOCX文件
     */
    private String parseDocx(InputStream inputStream) throws Exception {
        XWPFDocument document = new XWPFDocument(inputStream);
        StringBuilder text = new StringBuilder();
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            text.append(paragraph.getText()).append("\n");
        }
        document.close();
        return text.toString();
    }

    /**
     * 从文本中解析用户数据
     * 支持格式：
     * 一对一 2 号 500 会员
     * 性别：女
     * 学历：本科
     * ...
     * 联系方式：推荐人郭四平，18735351536，微信同号。
     */
    private List<UserData> parseUserData(String content) {
        List<UserData> result = new ArrayList<>();

        // 按空行分割成多个用户信息块
        String[] blocks = content.split("\\n\\s*\\n");

        for (String block : blocks) {
            if (block.trim().isEmpty()) {
                continue;
            }

            UserData userData = new UserData();
            String[] lines = block.split("\\n");

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // 会员编号（第一行）
                if (line.matches(".+\\d+\\s*号.+会员")) {
                    userData.memberNumber = line;
                    continue;
                }

                // 性别
                if (line.startsWith("性别")) {
                    String value = extractValue(line);
                    userData.gender = "男".equals(value) ? 1 : "女".equals(value) ? 2 : 0;
                    continue;
                }

                // 学历
                if (line.startsWith("学历")) {
                    userData.education = extractValue(line);
                    continue;
                }

                // 籍贯
                if (line.startsWith("籍贯")) {
                    userData.nativePlace = extractValue(line);
                    continue;
                }

                // 居住地
                if (line.startsWith("居住地")) {
                    userData.residence = extractValue(line);
                    continue;
                }

                // 出生年月（同时提取年龄）
                if (line.startsWith("出生年月")) {
                    userData.birthDate = extractValue(line);
                    // 提取年龄：1990 年（32 周岁）
                    Pattern agePattern = Pattern.compile("[\uff08\\(]?(\\d{1,2})\\s*周岁");
                    Matcher ageMatcher = agePattern.matcher(line);
                    if (ageMatcher.find()) {
                        userData.age = Integer.parseInt(ageMatcher.group(1));
                    }
                    continue;
                }

                // 身高
                if (line.startsWith("身高")) {
                    String value = extractValue(line);
                    Pattern heightPattern = Pattern.compile("(\\d{2,3})");
                    Matcher matcher = heightPattern.matcher(value);
                    if (matcher.find()) {
                        userData.height = (matcher.group(1));
                    }
                    continue;
                }

                // 体重
                if (line.startsWith("体重")) {
                    String value = extractValue(line);
                    Pattern weightPattern = Pattern.compile("(\\d{2,3})");
                    Matcher matcher = weightPattern.matcher(value);
                    if (matcher.find()) {
                        userData.weight = (matcher.group(1));
                    }
                    continue;
                }

                // 工作单位
                if (line.startsWith("工作单位")) {
                    userData.workUnit = extractValue(line);
                    continue;
                }

                // 月薪
                if (line.startsWith("月薪")) {
                    userData.salary = extractValue(line);
                    continue;
                }

                // 婚史
                if (line.startsWith("婚史")) {
                    userData.maritalStatus = extractValue(line);
                    continue;
                }

                // 房车情况
                if (line.startsWith("房车情况")) {
                    userData.houseCarStatus = extractValue(line);
                    continue;
                }

                // 家庭情况
                if (line.startsWith("家庭情况")) {
                    userData.familyInfo = extractValue(line);
                    continue;
                }

                // 健康状况
                if (line.startsWith("本人健康状况")) {
                    userData.healthStatus = extractValue(line);
                    continue;
                }

                // 择偶要求
                if (line.startsWith("择偶要求")) {
                    userData.mateRequirement = extractValue(line);
                    continue;
                }

                // 联系方式（推荐人信息）
                if (line.startsWith("联系方式")) {
                    // 格式：推荐人郭四平，18735351536，微信同号。
                    Pattern recommenderPattern = Pattern.compile("推荐人([^\uff0c,]+)");
                    Matcher recommenderMatcher = recommenderPattern.matcher(line);
                    if (recommenderMatcher.find()) {
                        userData.recommender = recommenderMatcher.group(1).trim();
                        // 使用推荐人姓名作为用户姓名（如果没有单独的姓名字段）
                        if (userData.name == null) {
                            userData.name = userData.recommender;
                        }
                    }

                    // 提取电话
                    Pattern phonePattern = Pattern.compile("(1[3-9]\\d{9})");
                    Matcher phoneMatcher = phonePattern.matcher(line);
                    if (phoneMatcher.find()) {
                        String phone = phoneMatcher.group(1);
                        // 第一个电话作为推荐人电话
                        if (userData.recommenderPhone == null) {
                            userData.recommenderPhone = phone;
                        }
                        // 如果没有用户电话，使用推荐人电话
                        if (userData.phone == null) {
                            userData.phone = phone;
                        }
                    }
                    continue;
                }
            }

            // 验证必须字段
            if (userData.phone != null && userData.phone.matches("1[3-9]\\d{9}")) {
                result.add(userData);
            } else {
                log.warn("跳过无效用户数据：" + userData.memberNumber);
            }
        }

        return result;
    }

    /**
     * 提取冒号后的值
     */
    private String extractValue(String line) {
        int colonIndex = line.indexOf('：');
        if (colonIndex == -1) {
            colonIndex = line.indexOf(':');
        }
        if (colonIndex != -1 && colonIndex < line.length() - 1) {
            return line.substring(colonIndex + 1).trim();
        }
        return "";
    }

    /**
     * 从 PDF 中提取图片
     */
    private List<byte[]> extractImagesFromPdf(InputStream inputStream) {
        List<byte[]> images = new ArrayList<>();
        try {
            PDDocument document = PDDocument.load(inputStream);
            for (PDPage page : document.getPages()) {
                PDResources resources = page.getResources();
                for (COSName name : resources.getXObjectNames()) {
                    if (resources.isImageXObject(name)) {
                        try {
                            PDImageXObject image = (PDImageXObject) resources.getXObject(name);
                            BufferedImage bImage = image.getImage();
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ImageIO.write(bImage, "jpg", baos);
                            images.add(baos.toByteArray());
                        } catch (Exception e) {
                            log.error("提取 PDF 图片失败", e);
                        }
                    }
                }
            }
            document.close();
        } catch (Exception e) {
            log.error("解析 PDF 图片失败", e);
        }
        return images;
    }

    /**
     * 从 DOCX 中提取图片
     */
    private List<byte[]> extractImagesFromDocx(InputStream inputStream) {
        List<byte[]> images = new ArrayList<>();
        try {
            XWPFDocument document = new XWPFDocument(inputStream);
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                for (XWPFRun run : paragraph.getRuns()) {
                    for (XWPFPicture picture : run.getEmbeddedPictures()) {
                        try {
                            images.add(picture.getPictureData().getData());
                        } catch (Exception e) {
                            log.error("提取 DOCX 图片失败", e);
                        }
                    }
                }
            }
            document.close();
        } catch (Exception e) {
            log.error("解析 DOCX 图片失败", e);
        }
        return images;
    }

    /**
     * 保存图片到文件系统
     * @param imageData 图片字节数据
     * @param filename 文件名（不含扩展名）
     * @return 图片 URL
     */
    private String saveImage(byte[] imageData, String filename) throws Exception {
        // 创建上传目录
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String dateDir = sdf.format(new Date());
        Path uploadDir = Paths.get(fileUploadPath, "avatars", dateDir);

        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // 保存文件
        String fullFilename = filename + ".jpg";
        Path filePath = uploadDir.resolve(fullFilename);
        Files.write(filePath, imageData);

        // 返回 URL
        return fileUploadUrlPrefix + "/upload/avatars/" + dateDir + "/" + fullFilename;
    }

    @Override
    public Map<String, Object> getHongniangStats(Integer hongniangId) {
        return calculateHongniangStats(hongniangId);
    }

    @Override
    public boolean canAccessTenant(Integer userId, Integer tenantId) {
        // 验证用户是否为红娘，且是否拥有访问指定租户的权限
        HongniangInfoEntity hongniang = this.getByUserId(userId);
        if (hongniang == null) {
            return false;
        }

        // 确保红娘的ID与租户ID匹配
        return hongniang.getId().equals(tenantId);
    }

    /**
     * 用户数据内部类
     */
    private static class UserData {
        String memberNumber;      // 会员编号（如：一对一 2 号 500 会员）
        String name;              // 姓名（从推荐人信息中提取）
        String phone;             // 电话
        Integer gender = 0;       // 0-未知 1-男 2-女
        Integer age;              // 年龄
        String education;         // 学历
        String nativePlace;       // 籍贯
        String residence;         // 居住地
        String birthDate;         // 出生年月
        String height;           // 身高(cm)
        String weight;           // 体重(斤)
        String workUnit;          // 工作单位
        String salary;            // 月薪
        String maritalStatus;     // 婚史
        String houseCarStatus;    // 房车情况
        String familyInfo;        // 家庭情况
        String healthStatus;      // 健康状况
        String mateRequirement;   // 择偶要求
        String recommender;       // 推荐人
        String recommenderPhone;  // 推荐人电话
        String avatarUrl;         // 头像 URL
    }

    private record DatabaseConnectionInfo(String host, int port, String database) {
    }

    private Map<String, Object> calculateHongniangStats(Integer hongniangId) {
        Map<String, Object> stats = new HashMap<>();
        if (hongniangId == null || hongniangId <= 0) {
            stats.put("totalUsers", 0);
            stats.put("totalActivities", 0);
            stats.put("successCount", 0);
            return stats;
        }

        List<Integer> relationUserIds = relationService.getUserIdsByHongniangId(hongniangId);
        Set<Integer> managedUserIds = relationUserIds == null
                ? new LinkedHashSet<>()
                : relationUserIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);

        List<XiangqinActivityEntity> activities = xiangqinActivityService.getByHongniangId(hongniangId);
        Set<Integer> linkedUserIds = new LinkedHashSet<>();
        if (activities != null) {
            for (XiangqinActivityEntity activity : activities) {
                if (activity == null || activity.getId() == null) {
                    continue;
                }
                List<XiangqinEnrollmentEntity> enrollments = xiangqinEnrollmentService.getByActivityId(activity.getId());
                if (enrollments == null) {
                    continue;
                }
                for (XiangqinEnrollmentEntity enrollment : enrollments) {
                    if (enrollment == null || enrollment.getUserId() == null || enrollment.getUserId() <= 0) {
                        continue;
                    }
                    Integer status = enrollment.getStatus();
                    if (status != null && (status == 2 || status == 3)) {
                        continue;
                    }
                    linkedUserIds.add(enrollment.getUserId());
                }
            }
        }

        stats.put("totalUsers", managedUserIds.size());
        stats.put("totalActivities", activities == null ? 0 : activities.size());
        int successCount = hongniangMatchCaseService.countSuccessCases(hongniangId);

        stats.put("successCount", successCount);
        return stats;
    }
}
