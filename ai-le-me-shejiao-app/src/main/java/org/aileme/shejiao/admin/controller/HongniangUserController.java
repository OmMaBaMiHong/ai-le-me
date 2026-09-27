package org.aileme.shejiao.admin.controller;

import cn.hutool.core.util.NumberUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.api.service.HongniangService;
import org.aileme.shejiao.api.service.HongniangUserRelationService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.Query;
import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangInfoEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangUserRelationEntity;

import java.util.ArrayList;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 红娘用户管理Controller
 *
 * @author system
 * @date 2026-01-29
 */
@RestController
@RequestMapping("/admin/hongniang-user")
@Tag(name = "管理端——红娘用户管理")
public class HongniangUserController {

    @Autowired
    private HongniangUserRelationService hongniangUserRelationService;

    @Autowired
    private HongniangService hongniangService;

    @Autowired
    private AppUserService appUserService;

    /**
     * 红娘用户关联列表
     */
    @GetMapping("/list")
    @Operation(summary = "红娘用户关联列表")
    public R list(@RequestParam Map<String, Object> params) {
        String hongniangName = trimToNull((String) params.get("hongniangName"));
        String userKeyword = firstNonBlank((String) params.get("userKeyword"), (String) params.get("userName"));
        Integer hongniangUserNo = parseInteger(params.get("hongniangUserNo"));

        Set<Integer> matchedHongniangIds = null;
        if (StringUtils.hasText(hongniangName)) {
            matchedHongniangIds = hongniangService.lambdaQuery()
                    .like(HongniangInfoEntity::getHongniangName, hongniangName)
                    .list()
                    .stream()
                    .map(HongniangInfoEntity::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (matchedHongniangIds.isEmpty()) {
                return R.ok().put("page", emptyPage(params));
            }
        }

        Set<Integer> matchedUserIds = null;
        if (StringUtils.hasText(userKeyword)) {
            LambdaQueryWrapper<AppUserEntity> userWrapper = new LambdaQueryWrapper<>();
            if (NumberUtil.isInteger(userKeyword)) {
                Integer userId = Integer.valueOf(userKeyword);
                userWrapper.and(wrapper -> wrapper.eq(AppUserEntity::getUid, userId)
                        .or().like(AppUserEntity::getUsername, userKeyword)
                        .or().like(AppUserEntity::getMobile, userKeyword));
            } else {
                userWrapper.and(wrapper -> wrapper.like(AppUserEntity::getUsername, userKeyword)
                        .or().like(AppUserEntity::getMobile, userKeyword));
            }
            matchedUserIds = appUserService.list(userWrapper)
                    .stream()
                    .map(AppUserEntity::getUid)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (matchedUserIds.isEmpty()) {
                return R.ok().put("page", emptyPage(params));
            }
        }

        LambdaQueryWrapper<HongniangUserRelationEntity> relationWrapper = new LambdaQueryWrapper<>();
        if (matchedHongniangIds != null) {
            relationWrapper.in(HongniangUserRelationEntity::getHongniangId, matchedHongniangIds);
        }
        if (matchedUserIds != null) {
            relationWrapper.in(HongniangUserRelationEntity::getUserId, matchedUserIds);
        }
        if (hongniangUserNo != null) {
            relationWrapper.eq(HongniangUserRelationEntity::getHongniangUserNo, hongniangUserNo);
        }
        relationWrapper.orderByDesc(HongniangUserRelationEntity::getCreateTime)
                .orderByDesc(HongniangUserRelationEntity::getId);

        IPage<HongniangUserRelationEntity> page = hongniangUserRelationService.page(
                new Query<HongniangUserRelationEntity>().getPage(params),
                relationWrapper
        );

        List<HongniangUserRelationEntity> relations = page.getRecords();
        if (relations.isEmpty()) {
            return R.ok().put("page", new PageUtils(List.of(), (int) page.getTotal(), (int) page.getSize(), (int) page.getCurrent()));
        }

        Map<Integer, HongniangInfoEntity> hongniangMap = hongniangService.listByIds(relations.stream()
                        .map(HongniangUserRelationEntity::getHongniangId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .stream()
                .collect(Collectors.toMap(HongniangInfoEntity::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        Map<Integer, AppUserEntity> userMap = appUserService.listByIds(relations.stream()
                        .map(HongniangUserRelationEntity::getUserId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .stream()
                .collect(Collectors.toMap(AppUserEntity::getUid, item -> item, (left, right) -> left, LinkedHashMap::new));

        List<Map<String, Object>> result = relations.stream()
                .map(relation -> buildRelationRow(relation, hongniangMap.get(relation.getHongniangId()), userMap.get(relation.getUserId())))
                .collect(Collectors.toList());

        PageUtils pageUtils = new PageUtils(result, (int) page.getTotal(), (int) page.getSize(), (int) page.getCurrent());
        return R.ok().put("page", pageUtils);
    }

    /**
     * 获取所有红娘列表
     */
    @GetMapping("/listAll")
    @Operation(summary = "获取所有红娘列表")
    public R listAll() {
        List<HongniangInfoEntity> hongniangs = hongniangService.list();
        return R.ok().put("list", hongniangs);
    }

    /**
     * 获取所有用户列表
     */
    @GetMapping("/listAllUsers")
    @Operation(summary = "获取所有用户列表")
    public R listAllUsers() {
        List<Map<String, Object>> users = appUserService.lambdaQuery()
                .orderByDesc(AppUserEntity::getUid)
                .last("limit 200")
                .list()
                .stream()
                .map(this::buildUserOption)
                .collect(Collectors.toList());
        return R.ok().put("list", users);
    }

    /**
     * 分配弹窗用户搜索
     */
    @GetMapping("/assignable-users")
    @Operation(summary = "搜索可分配用户")
    public R assignableUsers(@RequestParam(value = "keyword", required = false) String keyword,
                             @RequestParam(value = "hongniangId", required = false) Integer hongniangId,
                             @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit) {
        int actualLimit = Math.max(1, Math.min(limit == null ? 20 : limit, 50));
        String normalizedKeyword = trimToNull(keyword);

        LambdaQueryWrapper<AppUserEntity> userWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(normalizedKeyword)) {
            if (NumberUtil.isInteger(normalizedKeyword)) {
                Integer userId = Integer.valueOf(normalizedKeyword);
                userWrapper.and(wrapper -> wrapper.eq(AppUserEntity::getUid, userId)
                        .or().like(AppUserEntity::getUsername, normalizedKeyword)
                        .or().like(AppUserEntity::getMobile, normalizedKeyword));
            } else {
                userWrapper.and(wrapper -> wrapper.like(AppUserEntity::getUsername, normalizedKeyword)
                        .or().like(AppUserEntity::getMobile, normalizedKeyword));
            }
        }
        if (hongniangId != null && hongniangId > 0) {
            List<Integer> assignedUserIds = hongniangUserRelationService.getUserIdsByHongniangId(hongniangId);
            if (!assignedUserIds.isEmpty()) {
                userWrapper.notIn(AppUserEntity::getUid, assignedUserIds);
            }
        }
        userWrapper.orderByDesc(AppUserEntity::getUid).last("limit " + actualLimit);

        List<Map<String, Object>> users = appUserService.list(userWrapper)
                .stream()
                .map(this::buildUserOption)
                .collect(Collectors.toList());
        return R.ok().put("list", users);
    }

    /**
     * 添加红娘用户关联
     */
    @PostMapping("/add")
    @Operation(summary = "添加红娘用户关联")
    public R add(@RequestBody Map<String, Object> params) {
        Integer hongniangId = Integer.valueOf(params.get("hongniangId").toString());
        Integer userId = Integer.valueOf(params.get("userId").toString());

        if (hongniangService.getById(hongniangId) == null) {
            return R.error("红娘不存在");
        }
        if (appUserService.getById(userId) == null) {
            return R.error("用户不存在");
        }

        // 检查是否已存在关联
        if (hongniangUserRelationService.checkFollowed(userId, hongniangId)) {
            return R.error("该用户与红娘已存在关联关系");
        }

        // 创建关联关系
        HongniangUserRelationEntity relation = new HongniangUserRelationEntity();
        relation.setHongniangId(hongniangId);
        relation.setUserId(userId);
        relation.setSourceType(3); // 3-系统分配
        relation.setCreateTime(new Date());
        relation.setRemark("系统分配");

        hongniangUserRelationService.saveRelationWithAutoNo(relation);
        hongniangService.updateStatistics(hongniangId);

        return R.ok().put("msg", "关联成功");
    }

    /**
     * 删除红娘用户关联
     */
    @PostMapping("/delete")
    @Operation(summary = "删除红娘用户关联")
    public R delete(@RequestBody Integer[] ids) {
        List<Integer> hongniangIds = hongniangUserRelationService.listByIds(Arrays.asList(ids)).stream()
                .map(HongniangUserRelationEntity::getHongniangId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        hongniangUserRelationService.removeByIds(Arrays.asList(ids));
        hongniangIds.forEach(hongniangService::updateStatistics);
        return R.ok().put("msg", "删除成功");
    }

    /**
     * 更新红娘用户关联
     */
    @PutMapping("/update")
    @Operation(summary = "更新红娘用户关联")
    public R update(@RequestBody HongniangUserRelationEntity relation) {
        HongniangUserRelationEntity existed = hongniangUserRelationService.getById(relation.getId());
        if (existed == null) {
            return R.error("关联关系不存在");
        }
        Integer targetHongniangId = relation.getHongniangId() != null ? relation.getHongniangId() : existed.getHongniangId();
        Integer targetUserId = relation.getUserId() != null ? relation.getUserId() : existed.getUserId();
        if ((!Objects.equals(targetHongniangId, existed.getHongniangId()) || !Objects.equals(targetUserId, existed.getUserId()))
                && hongniangUserRelationService.checkFollowed(targetUserId, targetHongniangId)) {
            return R.error("该用户与红娘已存在关联关系");
        }
        if (relation.getHongniangUserNo() == null || relation.getHongniangUserNo() <= 0) {
            relation.setHongniangUserNo(existed.getHongniangUserNo() != null && existed.getHongniangUserNo() > 0
                    ? existed.getHongniangUserNo()
                    : hongniangUserRelationService.getNextHongniangUserNo(targetHongniangId));
        }
        hongniangUserRelationService.updateById(relation);
        if (existed.getHongniangId() != null) {
            hongniangService.updateStatistics(existed.getHongniangId());
        }
        if (targetHongniangId != null && !Objects.equals(targetHongniangId, existed.getHongniangId())) {
            hongniangService.updateStatistics(targetHongniangId);
        }
        return R.ok().put("msg", "更新成功");
    }

    /**
     * 获取关联详情
     */
    @GetMapping("/info/{id}")
    @Operation(summary = "获取关联详情")
    public R info(@PathVariable("id") Long id) {
        HongniangUserRelationEntity relation = hongniangUserRelationService.getById(id);
        return R.ok().put("hongniangUser", relation);
    }

    private PageUtils emptyPage(Map<String, Object> params) {
        Integer pageSize = parseInteger(params.get("pageSize"));
        Integer pageNum = parseInteger(params.get("pageNum"));
        return new PageUtils(List.of(), 0, pageSize == null || pageSize < 1 ? 10 : pageSize, pageNum == null || pageNum < 1 ? 1 : pageNum);
    }

    private Map<String, Object> buildRelationRow(HongniangUserRelationEntity relation,
                                                 HongniangInfoEntity hongniang,
                                                 AppUserEntity user) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", relation.getId());
        item.put("hongniangId", relation.getHongniangId());
        item.put("hongniangUserNo", relation.getHongniangUserNo());
        item.put("userId", relation.getUserId());

        if (hongniang != null) {
            item.put("hongniangName", hongniang.getHongniangName());
            item.put("hongniangPhone", hongniang.getPhone());
            item.put("wechat", hongniang.getWechat());
            item.put("companyName", hongniang.getCompanyName());
            item.put("level", hongniang.getLevel());
            item.put("totalUsers", hongniang.getTotalUsers());
            item.put("totalActivities", hongniang.getTotalActivities());
            item.put("hongniangStatus", hongniang.getStatus());
            item.put("hongniangCreateTime", hongniang.getCreateTime());
            item.put("hongniangUpdateTime", hongniang.getUpdateTime());
        }

        if (user != null) {
            item.put("username", user.getUsername());
            item.put("userMobile", user.getMobile());
            item.put("userGender", user.getGender());
            item.put("userAge", user.getAge());
            item.put("avatar", user.getAvatar());
            item.put("intro", user.getIntro());
            item.put("integral", user.getIntegral());
            item.put("money", user.getMoney());
            item.put("vip", user.getVip());
            item.put("type", user.getType());
            item.put("userStatus", user.getStatus());
            item.put("userCreateTime", user.getCreateTime());
            item.put("userUpdateTime", user.getUpdateTime());
        }

        item.put("relationTime", relation.getCreateTime());
        item.put("sourceType", relation.getSourceType());
        item.put("importFileName", relation.getImportFileName());
        item.put("remark", relation.getRemark());
        return item;
    }

    private Map<String, Object> buildUserOption(AppUserEntity user) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("uid", user.getUid());
        item.put("username", user.getUsername());
        item.put("mobile", user.getMobile());
        item.put("gender", user.getGender());
        item.put("age", user.getAge());
        item.put("hongniangId", user.getHongniangId());
        return item;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        if (!NumberUtil.isInteger(text)) {
            return null;
        }
        return Integer.valueOf(text);
    }
}
