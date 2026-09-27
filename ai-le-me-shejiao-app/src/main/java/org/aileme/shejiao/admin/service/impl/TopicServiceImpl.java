/**
 * -----------------------------------
 *  Copyright (c) 2021-2024
 *  All rights reserved, Designed By www.linfengtech.cn
 *  林风社交论坛商业版本请务必保留此注释头信息
 *  商业版授权联系技术客服	 QQ:  3582996245
 *  严禁分享、盗用、转卖源码或非法牟利！
 *  版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.admin.service.impl;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.dynamic.datasource.annotation.DS;

import java.time.Duration;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.domain.vo.*;
import org.aileme.shejiao.common.utils.*;
import org.aileme.shejiao.admin.utils.WechatUtil;import org.aileme.shejiao.domain.entity.admin.*;
import org.aileme.shejiao.api.service.*;
import org.aileme.shejiao.domain.entity.app.TopicAdminEntity;
import org.aileme.shejiao.domain.entity.app.TopicApplyEntity;
import org.aileme.shejiao.domain.entity.app.TopicTopEntity;
import org.aileme.shejiao.domain.entity.app.UserTopicEntity;
import org.aileme.shejiao.domain.param.app.*;
import org.aileme.shejiao.admin.utils.LocalUser;
import org.aileme.shejiao.common.utils.QRCodeUtils;
import org.aileme.shejiao.app.oss.factory.OSSFactory;
import org.aileme.shejiao.api.service.SysConfigService;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.aileme.shejiao.admin.dao.TopicDao;


@DS("master")
@Service("topicService")
public class TopicServiceImpl extends ServiceImpl<TopicDao, TopicEntity> implements TopicService {

    private static final Integer DEFAULT_TOPIC_OWNER_UID = 1;

    @Autowired
    @Lazy
    private PostService postService;

    @Autowired
    private UserTopicService userTopicService;

    @Autowired
    private TopicAdminService topicAdminService;

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private DiscussService discussService;

    @Autowired
    private SensitiveService sensitiveService;

    @Autowired
    private LocalUser localUser;

    @Autowired
    @Qualifier("appLocalUser")
    private org.aileme.shejiao.app.utils.LocalUser appLocalUser;

    @Autowired
    private TopicTopService topicTopService;

    @Autowired
    private SysConfigService configService;
    @Autowired
    private WechatRuntimeConfigService wechatRuntimeConfigService;
    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private TopicBlockService topicBlockService;

    @Autowired
    private TopicApplyService topicApplyService;

    @Autowired
    private HongniangService hongniangService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<TopicEntity> queryWrapper=new QueryWrapper<>();
        //条件查询
        String key = (String)params.get("key");
        String status = (String)params.get("status");
        if(!WechatUtil.isEmpty(key)){
            if(NumberUtil.isInteger(key)){
                queryWrapper.lambda().eq(TopicEntity::getUid,key);
            }else{
                queryWrapper.lambda()
                        .like(TopicEntity::getTopicName, key)
                        .or()
                        .like(TopicEntity::getDescription,key);
            }
        }
        if(!WechatUtil.isEmpty(status)){
            queryWrapper.lambda().eq(TopicEntity::getStatus,status);
        }
        IPage<TopicEntity> page = this.page(
                new Query<TopicEntity>().getPage(params),queryWrapper
        );
        List<TopicEntity> list = page.getRecords();
        List<TopicListAdminResponse> responseList=new ArrayList<>();
        list.forEach(topic->{
            TopicListAdminResponse response=new TopicListAdminResponse();
            BeanUtils.copyProperties(topic,response);
            CategoryEntity category= categoryService.getById(topic.getCateId());
            response.setCateName(category.getCateName());
            AppUserEntity user = appUserService.getById(topic.getUid());
            response.setAvatar(user.getAvatar());
            response.setUserInfo(user);
            responseList.add(response);
        });
        PageUtils pageUtils=new PageUtils(page);
        pageUtils.setList(responseList);
        return pageUtils;
    }

    @Override
    public AppPageUtils queryByPageList(Map<String, Object> params) {
        AppPageUtils page = this.queryByPage(params);
        List<TopicEntity> list = (List<TopicEntity>)page.getData();
        page.setData(toTopicListResponseList(list));
        return page;
    }

    @Override
    public List<TopicListResponse> hotTopic() {
        LambdaQueryWrapper<TopicEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TopicEntity::getStatus, Constant.NORMAL);
        applyTopicDisplayOrder(queryWrapper);
        queryWrapper.last("limit 30");
        return toTopicListResponseList(this.list(queryWrapper));
    }

    @Override
    public AppPageUtils classTopicAreImg(Integer classId, Integer page) {

        QueryWrapper<TopicEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TopicEntity::getStatus,Constant.NORMAL);
        if(classId!=null){
            queryWrapper.lambda().eq(TopicEntity::getCateId, classId);
        }
        //只查询圈子人数大于10
//        queryWrapper.gt("user_num", 10);
        queryWrapper.lambda().orderByDesc(TopicEntity::getUserNum);
        Page<TopicEntity> pages = new Page<>(page,10);
        Page<TopicEntity> page1 = this.page(pages, queryWrapper);
        AppPageUtils appPageUtils=new AppPageUtils(page1);
        if(page1.getRecords().isEmpty()){
            return appPageUtils;
        }
        List<TopicEntity> list = (List<TopicEntity>) appPageUtils.getData();
        List<ClassTopicImageResponse> responseList=new ArrayList<>();
        List<Integer> topicIdList = list.stream().map(TopicEntity::getId).collect(Collectors.toList());
        List<TopicPostResponse> countBatch = postService.findTopicPostCountBatch(topicIdList);
        Map<Integer, Integer> countMap = countBatch.stream().collect(Collectors.toMap(TopicPostResponse::getTopicId,TopicPostResponse::getNumber));
        list.forEach(l->{
            ClassTopicImageResponse response=new ClassTopicImageResponse();
            BeanUtils.copyProperties(l,response);
            if(ObjectUtil.isNotNull(countMap.get(l.getId()))){
                response.setPostNum(countMap.get(l.getId()));
            }else{
                response.setPostNum(0);
            }
            response.setImgList(postService.findThreeMedia(l.getId()));
            responseList.add(response);
        });
        appPageUtils.setData(responseList);
        return appPageUtils;
    }

    @Override
    public AppPageUtils myCreateTopic(Integer currPage,AppUserEntity user) {

        Page<TopicEntity> page = new Page<>(currPage,10);
        QueryWrapper<TopicEntity> queryWrapper=new QueryWrapper<>();
        queryWrapper.lambda().eq(TopicEntity::getUid,user.getUid());
        queryWrapper.lambda().orderByDesc(TopicEntity::getId);
        return this.mapTopicList(page,queryWrapper);
    }

    @Override
    @DSTransactional(rollbackFor = Exception.class)
    public Integer topicAdd(TopicAddForm topic,AppUserEntity user) {
        checkAddTopicAuth(user);
        List<TopicEntity> list = this.lambdaQuery().eq(TopicEntity::getTopicName, topic.getTopicName()).list();
        if(list.size()>0){
            throw new LinfengException("圈子名称已存在请重新命名");
        }
        Boolean sensitiveDesc = sensitiveService.checkContent(topic.getDescription());
        if(sensitiveDesc){
            throw new LinfengException("描述含有敏感词请修改");
        }
        Boolean sensitiveName = sensitiveService.checkContent(topic.getTopicName());
        if(sensitiveName){
            throw new LinfengException("标题含有敏感词请修改");
        }
        Date date = DateUtil.nowDateTime();
        TopicEntity topicEntity=new TopicEntity();
        BeanUtils.copyProperties(topic,topicEntity);
        topicEntity.setCreateTime(date);
        topicEntity.setUid(user.getUid());
        Integer hongniangId = resolveHongniangId(user);
        if (hongniangId != null && hongniangId > 0) {
            topicEntity.setChannelType(2);
            topicEntity.setHongniangId(hongniangId);
        } else {
            topicEntity.setChannelType(3);
            topicEntity.setHongniangId(null);
        }
        if(topic.getRest()){
            topicEntity.setRest(1);
            topicEntity.setQuestion(topic.getQuestion());
        }else{
            topicEntity.setRest(0);
        }
        String value = configService.getValue(Constant.CIRCLE_CHECK);
        topicEntity.setStatus(Integer.valueOf(value));
        boolean save = this.save(topicEntity);
        if(save) {
            UserTopicEntity userTopic=new UserTopicEntity();
            userTopic.setUid(user.getUid());
            userTopic.setTopicId(topicEntity.getId());
            userTopic.setCreateTime(date);
            userTopicService.save(userTopic);
            //圈子管理员表添加记录
            TopicAdminEntity topicAdmin =new TopicAdminEntity();
            topicAdmin.setCreateTime(DateUtil.nowDateTime());
            topicAdmin.setTopicId(topicEntity.getId());
            topicAdmin.setUid(user.getUid());
            topicAdminService.save(topicAdmin);
            return topicEntity.getId();
        }
        return 0;
    }

    @Override
    public AppPageUtils search(Integer currPage, String keyword) {
        if(currPage==1){
            if(localUser!=null){
                Integer uid = localUser.getUser().getUid();
                searchService.setSearchContent(keyword,uid);
            }
        }
        QueryWrapper<TopicEntity> queryWrapper=new QueryWrapper<>();
        queryWrapper.lambda().like(TopicEntity::getTopicName, keyword).or().like(TopicEntity::getDescription, keyword);
        queryWrapper.lambda().orderByDesc(TopicEntity::getUserNum);
        Page<TopicEntity> p = new Page<>(currPage,10);
        return this.mapTopicList(p,queryWrapper);
    }

    @Override
    public Boolean topicEdit(TopicUpdateForm topic, AppUserEntity user) {
        //判断修改用户是否为圈子管理员
        Boolean isAdmin = topicAdminService.isAdmin(user.getUid(), topic.getId());
        if(!isAdmin){
            throw new LinfengException("你不是圈子管理员");
        }
        Boolean sensitiveDesc = sensitiveService.checkContent(topic.getDescription());
        Boolean sensitiveName = sensitiveService.checkContent(topic.getTopicName());
        if(sensitiveDesc){
            throw new LinfengException("描述含有敏感词请修改");
        }
        if(sensitiveName){
            throw new LinfengException("标题含有敏感词请修改");
        }
        TopicEntity topicEntity = this.getById(topic.getId());
        if(ObjectUtil.isNull(topicEntity)){
            return false;
        }
        //如果改动了圈子私密状态 则批量处理对应帖子
        if(!topicEntity.getIsPrivacy().equals(topic.getIsPrivacy())){
            updatePostStatusBatch(topic.getIsPrivacy(),topicEntity.getId());
        }
        topicEntity.setIsPrivacy(topic.getIsPrivacy());

        if(ObjectUtil.isNotNull(topic.getBgImage())){
            topicEntity.setBgImage(topic.getBgImage());
        }
        if(ObjectUtil.isNotNull(topic.getCoverImage())){
            topicEntity.setCoverImage(topic.getCoverImage());
        }
        if(ObjectUtil.isNotNull(topic.getDescription())){
            topicEntity.setDescription(topic.getDescription());
        }
        if(ObjectUtil.isNotNull(topic.getTopicName())){
            List<TopicEntity> list = this.lambdaQuery().eq(TopicEntity::getTopicName, topic.getTopicName()).list();
            if(list.size()>1){
                throw new LinfengException("圈子名称已存在请重新命名");
            }else if(list.size()==1){
                TopicEntity topics = list.get(0);
                if(!topics.getId().equals(topic.getId())){
                    throw new LinfengException("圈子名称已存在请重新命名");
                }
            }
            topicEntity.setTopicName(topic.getTopicName());
        }
        if(ObjectUtil.isNotNull(topic.getRest())){
            if(topic.getRest()){
                topicEntity.setRest(1);
                if(WechatUtil.isEmpty(topic.getQuestion())){
                    throw new LinfengException("进圈问答不能为空");
                }
                topicEntity.setQuestion(topic.getQuestion());
            }else{
                topicEntity.setRest(0);
            }
        }
        return this.updateById(topicEntity);
    }

    @Override
    public boolean detection(Integer uid) {
        AppUserEntity user = appUserService.getById(uid);
        AppUserEntity appUser = appUserService.vipExpirationCheck(user);
        checkAddTopicAuth(appUser);
        return true;
    }

    @Override
    public AppPageUtils queryByPage(Map<String, Object> params) {
        QueryWrapper<TopicEntity> queryWrapper = new QueryWrapper<>();
        Integer classId = parseIntegerParam(params.get(Constant.CLASSID));
        String keyword = ObjectUtil.isNull(params.get("keyword")) ? null : String.valueOf(params.get("keyword")).trim();

        queryWrapper.lambda().eq(TopicEntity::getStatus, Constant.NORMAL);
        if (ObjectUtil.isNotNull(classId)) {
            queryWrapper.lambda().eq(TopicEntity::getCateId, classId);
        }
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.lambda()
                    .and(wrapper -> wrapper.like(TopicEntity::getTopicName, keyword)
                            .or()
                            .like(TopicEntity::getDescription, keyword));
        }
        applyTopicDisplayOrder(queryWrapper);

        IPage<TopicEntity> page = this.page(
                new Query<TopicEntity>().getPage(params),
                queryWrapper
        );
        return new AppPageUtils(page);
    }

    private Integer parseIntegerParam(Object value) {
        if (ObjectUtil.isNull(value)) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = String.valueOf(value).trim();
        if (StrUtil.isBlank(text)) {
            return null;
        }
        return Integer.parseInt(text);
    }

    @Override
    public TopicDetailResponse detail(Integer id) {
        TopicDetailResponse response=new TopicDetailResponse();
        TopicEntity topicEntity = this.getById(id);
        if(ObjectUtil.isNull(topicEntity)){
            throw new LinfengException("该圈子不存在");
        }
        if(topicEntity.getStatus().equals(Constant.BAN)){
            throw new LinfengException("该圈子已被禁用");
        }
        if(topicEntity.getStatus().equals(Constant.TOPIC_AUDIT)){
            throw new LinfengException("该圈子审核中");
        }
        AppUserEntity user = localUser.getUser();

        // 尝试从App端获取用户（Admin端session可能为空）
        if (ObjectUtil.isNull(user)) {
            user = appLocalUser.getUser();
        }

        BeanUtils.copyProperties(topicEntity,response);
        response.setIsOfficial(isOfficialTopic(topicEntity));
        Integer topicId=topicEntity.getId();
        Integer postCount = postService.findTopicPostCount(topicId);
        response.setPostCount(postCount);
//        Integer userCount=userTopicService.findUserTopicService(topicId);
        response.setUserCount(topicEntity.getUserNum());
        if(ObjectUtil.isNull(user)){
            response.setIsJoin(false);
            response.setIsAdmin(false);
        }else{
            Boolean join=userTopicService.isJoin(user.getUid(),topicId);
            response.setIsJoin(join);
            Boolean isAdmin=topicAdminService.isAdmin(user.getUid(),topicId);
            response.setIsAdmin(isAdmin);
        }
        AppUserEntity appUser = resolveTopicOwnerUser(topicEntity);
        AppUserShortInfoResponse shortInfoResponse = buildTopicOwnerShortInfo(topicEntity, appUser);
        response.setUid(shortInfoResponse.getUid());
        response.setUserInfo(shortInfoResponse);
        response.setHongniangInfo(buildHongniangTopicInfo(topicEntity));
        List<TopicTopEntity> topicTopList=topicTopService.findByTopicId(topicId);
        List<Integer> postIdList = topicTopList.stream().map(TopicTopEntity::getPostId).collect(Collectors.toList());
        if(postIdList.isEmpty()){
            response.setTopPost(new ArrayList<>());
        }else{
            List<PostEntity> postEntityList = postService.lambdaQuery()
                    .eq(PostEntity::getStatus, Constant.NORMAL)
                    .in(PostEntity::getId,postIdList)
                    .list();
            response.setTopPost(postEntityList);
        }
        List<DiscussEntity> discussList=discussService.getListByTopicId(topicId);
        response.setDiscussList(discussList);
        //设置圈子用户
        List<AppUserShortInfoResponse> userList=new ArrayList<>();
        List<Integer> uidListByTopicId = userTopicService.getSomeUidListByTopicId(id);
        List<AppUserEntity> batchUserList = appUserService.getBatchUser(uidListByTopicId);
        batchUserList.forEach(item->{
            AppUserShortInfoResponse vo=new AppUserShortInfoResponse();
            BeanUtils.copyProperties(item,vo);
            userList.add(vo);
        });
        response.setUserList(userList);
        //设置圈子管理员
        List<TopicAdminEntity> list = topicAdminService.lambdaQuery().eq(TopicAdminEntity::getTopicId, id).orderByAsc(TopicAdminEntity::getId).list();
        List<Integer> uidList = list.stream().map(TopicAdminEntity::getUid).collect(Collectors.toList());
        if(uidList.size()==0){
            response.setAdminList(new ArrayList<>());
        }else{
            List<AppUserEntity> batchUser = appUserService.getBatchUser(uidList);
            List<AppUserShortInfoResponse> adminList=new ArrayList<>();
            batchUser.forEach(u->{
                AppUserShortInfoResponse vo=new AppUserShortInfoResponse();
                BeanUtils.copyProperties(u,vo);
                adminList.add(vo);
            });
            response.setAdminList(adminList);
        }
        return response;
    }

    /**
     * 入圈请求
     * @param id
     * @param user
     * @return
     */
    @Override
    @DSTransactional
    public Integer joinTopic(Integer id, AppUserEntity user) {
        TopicEntity topicEntity = this.getById(id);
        Integer uid = user.getUid();
        Boolean join = userTopicService.isJoin(uid, id);
        if(join){
            throw new LinfengException("您已加入该圈子");
        }
        //黑名单判断
        Boolean isBlock = topicBlockService.isBlock(id, uid);
        if(isBlock){
            throw new LinfengException("圈主禁止你的加群申请");
        }
        if(topicEntity.getRest()==1){
            //检查是否为待审核
            Boolean isApply = topicApplyService.getApplyInfoByUserId(user.getUid(),id);
            if(isApply){
                return 2;
            }
            return 1;
        }
        UserTopicEntity userTopicEntity=new UserTopicEntity();
        userTopicEntity.setCreateTime(DateUtil.nowDateTime());
        userTopicEntity.setTopicId(id);
        userTopicEntity.setUid(uid);
        userTopicService.save(userTopicEntity);

        topicEntity.setUserNum(topicEntity.getUserNum()+1);
        this.updateById(topicEntity);
        return 0;
    }

    @Override
    @DSTransactional
    public void userTopicDel(Integer id, AppUserEntity user) {
        TopicEntity topicEntity = this.getById(id);
        if(topicEntity.getUid().equals(user.getUid())){
            throw new LinfengException("圈主只能解散圈子不能退出圈子");
        }
        userTopicService.remove(new LambdaQueryWrapper<UserTopicEntity>()
                .eq(UserTopicEntity::getTopicId,id)
                .eq(UserTopicEntity::getUid,user.getUid()));
        //圈子人数更新
        topicEntity.setUserNum(topicEntity.getUserNum()-1);
        this.updateById(topicEntity);

    }

    @Override
    @DSTransactional
    public void topicDel(Integer id, AppUserEntity user) {
        TopicEntity topicEntity = this.getById(id);
        if(!topicEntity.getUid().equals(user.getUid())){
            throw new LinfengException("不是圈主不能解散圈子");
        }
        if(id.equals(Constant.OFFICIAL_TOPIC_ID)){
            throw new LinfengException("官方圈子不能解散");
        }
        //解散圈子后的其他业务
        boolean remove = this.removeById(id);
        if(remove){
            //删除该圈子用户信息
            QueryWrapper<UserTopicEntity> queryWrapper=new QueryWrapper<>();
            queryWrapper.lambda().eq(UserTopicEntity::getTopicId,id);
            userTopicService.remove(queryWrapper);
            //删除该圈子相应话题
            QueryWrapper<DiscussEntity> wrapper=new QueryWrapper<>();
            wrapper.lambda().eq(DiscussEntity::getTopicId,id);
            discussService.remove(wrapper);
            //删除相关消息
            List<PostEntity> postList = postService.lambdaQuery().eq(PostEntity::getTopicId, id).list();
            if(postList.size()>0){
                List<Integer> postIdList = postList.stream().map(PostEntity::getId).collect(Collectors.toList());
                postIdList.forEach(postId->{
                    LambdaQueryWrapper<MessageEntity> query = new LambdaQueryWrapper<>();
                    query.eq(MessageEntity::getPostId,postId);
                    messageService.remove(query);
                });
            }
            //删除该圈子相应帖子
            QueryWrapper<PostEntity> postQueryWrapper=new QueryWrapper<>();
            postQueryWrapper.lambda().eq(PostEntity::getTopicId,id);
            postService.remove(postQueryWrapper);


        }

    }

    @Override
    @DSTransactional
    public void topicDeleteByAdmin(List<Integer> list) {
        list.forEach(id->{
            if(id.equals(Constant.OFFICIAL_TOPIC_ID)){
                throw new LinfengException("官方圈子不能解散");
            }
            //解散圈子后的其他业务
            boolean remove = this.removeById(id);
            if(remove){
                //删除该圈子用户信息
                QueryWrapper<UserTopicEntity> queryWrapper=new QueryWrapper<>();
                queryWrapper.lambda().eq(UserTopicEntity::getTopicId,id);
                userTopicService.remove(queryWrapper);
                //删除该圈子相应话题
                QueryWrapper<DiscussEntity> wrapper=new QueryWrapper<>();
                wrapper.lambda().eq(DiscussEntity::getTopicId,id);
                discussService.remove(wrapper);
                //删除相关消息
                List<PostEntity> postList = postService.lambdaQuery().eq(PostEntity::getTopicId, id).list();
                if(postList.size()>0){
                    List<Integer> postIdList = postList.stream().map(PostEntity::getId).collect(Collectors.toList());
                    postIdList.forEach(postId->{
                        LambdaQueryWrapper<MessageEntity> query = new LambdaQueryWrapper<>();
                        query.eq(MessageEntity::getPostId,postId);
                        messageService.remove(query);
                    });
                }
                //删除该圈子相应帖子
                QueryWrapper<PostEntity> postQueryWrapper=new QueryWrapper<>();
                postQueryWrapper.lambda().eq(PostEntity::getTopicId,id);
                postService.remove(postQueryWrapper);
            }
        });
    }

    @Override
    public AppPageUtils userJoinTopic(UserJoinTopicForm request, AppUserEntity user) {
        List<UserTopicEntity> topicList;
        if(ObjectUtil.isNotNull(request.getUid())){
            topicList=userTopicService.getTopicIdByUid(request.getUid());
        }else{
            topicList=userTopicService.getTopicIdByUid(user.getUid());
        }
        List<Integer> topicIdList = topicList.stream().map(UserTopicEntity::getTopicId).collect(Collectors.toList());
        if(topicIdList.isEmpty()){
            return new AppPageUtils(null, 0, 20, 1);
        }
        Integer currPage=1;
        if(ObjectUtil.isNotNull(request.getPage())){
            currPage=request.getPage();
        }
        Page<TopicEntity> page = new Page<>(currPage,20);
        QueryWrapper<TopicEntity> queryWrapper=new QueryWrapper<>();
        queryWrapper.lambda().in(TopicEntity::getId,topicIdList);
        if(ObjectUtil.isNotNull(request.getClassId())){
            queryWrapper.lambda().eq(TopicEntity::getCateId,request.getClassId());
        }

        applyTopicDisplayOrder(queryWrapper);
        return this.mapTopicList(page,queryWrapper);
    }


    /**
     * 生成圈子页面分享海报
     * @param topicId
     * @param origin
     * @param url
     * @return
     */
    @Override
    public String getQrCode(Integer topicId, String origin, String url,AppUserEntity user)throws Exception  {
        String result="";
        if(origin.equals(Constant.H5)){
            result = org.aileme.common.redis.utils.RedisUtils.getCacheObject(ConfigConstant.H5_TOPIC_POSTER_KEY + topicId);
        }else if(origin.equals(Constant.WX)){
            result = org.aileme.common.redis.utils.RedisUtils.getCacheObject(ConfigConstant.WX_TOPIC_POSTER_KEY + topicId);
        }
        if(!WechatUtil.isEmpty(result)){
            return result;
        }

        TopicEntity topic = this.getById(topicId);
        String showPic=topic.getBgImage();
        String itemName=topic.getDescription();
        int width = 640;
        int height = 1052;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = image.createGraphics();

        image = g2d.getDeviceConfiguration().createCompatibleImage(width, height, Transparency.OPAQUE);
        g2d.dispose();
        g2d = image.createGraphics();

        g2d.setBackground(Color.WHITE);
        g2d.clearRect(0, 0, width, height);

        g2d.setColor(new Color(182,249,225));
        g2d.setStroke(new BasicStroke(1));
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        BufferedImage proImg = WechatUtil.readImgInner(showPic);
        if(proImg==null){
            throw new LinfengException("分享图片不是https格式");
        }
        //关于高度不变 宽度等比例缩放
        int originalWidth = proImg.getWidth();
        int originalHeight = proImg.getHeight();
        double scale = (double) 600 / originalHeight;
        int newWidth = (int) (originalWidth * scale);
        int newHeight = 600;
        int horizontalOffset = (width - newWidth) / 2;
        g2d.drawImage(proImg.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH), horizontalOffset, 0, null);


        BufferedImage shareHeadImg = WechatUtil.readImgInner(topic.getCoverImage());
        if(shareHeadImg==null){
            throw new LinfengException("logo图片不是https格式");
        }
        int baseHeight = 610;


        g2d.setPaint(Color.GRAY);
        File newFileT = new File("simsunb.ttf");
        if(!newFileT.exists()){
            InputStream streamT =  getClass().getClassLoader()
                    .getResourceAsStream("simsunb.ttf");
            FileUtils.copyInputStreamToFile(streamT, newFileT);
        }
        Font font =  Font.createFont(Font.TRUETYPE_FONT, newFileT);
        g2d.setFont(font.deriveFont(Font.PLAIN,27));
        g2d.drawImage(shareHeadImg.getScaledInstance(120, 120, Image.SCALE_SMOOTH), 50, baseHeight, null);
        g2d.drawString(topic.getTopicName(), 210, baseHeight + 60);
        //简介
        g2d.setPaint(Color.BLACK);

        final Font logoFont = font.deriveFont(Font.PLAIN,21);
        g2d.setFont(logoFont);
        FontMetrics fm2 = g2d.getFontMetrics(g2d.getFont());
        int textWidth2 = fm2.stringWidth(itemName);
        while(textWidth2 > 260 && itemName.length() > 2) {
            itemName = itemName.substring(0,itemName.length()-1);
            textWidth2 = fm2.stringWidth(itemName);
        }
        g2d.setPaint(Color.GRAY);
        final Font logos = font.deriveFont(Font.BOLD,30);
        g2d.setFont(logos);
        g2d.drawString(itemName, 50, baseHeight + 190);
        Font fonts =  Font.createFont(Font.TRUETYPE_FONT, newFileT);
        g2d.setPaint(Color.BLACK);
        g2d.setFont(fonts.deriveFont(Font.BOLD,17));
        BufferedImage qrCode = null;
        if(origin.equals(Constant.H5)){
            g2d.drawString("扫描二维码查看圈子", 30, baseHeight + 336);
            qrCode = QRCodeUtils.createImage(url, null, true);
        }else if(origin.equals(Constant.WX)){
            g2d.drawString("长按识别小程序查看圈子", 30, baseHeight + 336);
            String appId = wechatRuntimeConfigService.getMiniAppId();
            String appSecret = wechatRuntimeConfigService.getMiniAppSecret();
            String accessToken = WechatUtil.getAccessToken(appId, appSecret);
            qrCode = WechatUtil.getWxCode(accessToken, url);
        }
        if (qrCode != null) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            g2d.drawImage(qrCode.getScaledInstance(160, 160, Image.SCALE_SMOOTH), 448, baseHeight + 266, null);
        }
        g2d.dispose();
        InputStream inputStream = WechatUtil.bufferedImageToInputStream(image);
        String resultUrl = OSSFactory.build().uploadSuffix(inputStream, "topic");
        //保存文件信息至redis缓存
        if(origin.equals(Constant.H5)){
            org.aileme.common.redis.utils.RedisUtils.setCacheObject(ConfigConstant.H5_TOPIC_POSTER_KEY+topicId, resultUrl, Duration.ofSeconds(60*60*24*15));
        }else if(origin.equals(Constant.WX)){
            org.aileme.common.redis.utils.RedisUtils.setCacheObject(ConfigConstant.WX_TOPIC_POSTER_KEY+topicId, resultUrl, Duration.ofSeconds(60*60*24*15));
        }

        return resultUrl;
    }

    @Override
    @DSTransactional
    public Boolean giveTopic(SetAdminForm request, AppUserEntity user) {
        if(request.getUid().equals(user.getUid())){
            throw new LinfengException("不能转让给自己");
        }
        TopicEntity topic = this.getById(request.getTopicId());
        if(!topic.getUid().equals(user.getUid())){
            throw new LinfengException("你不是圈主没有权限");
        }
        AppUserEntity userEntity = appUserService.getById(request.getUid());
        if(userEntity==null){
            throw new LinfengException("转让用户不存在");
        }
        //判断该用户是否为管理员
        Boolean isAdmin = topicAdminService.isAdmin(request.getUid(), request.getTopicId());
        if(!isAdmin){
            TopicAdminEntity topicAdmin=new TopicAdminEntity();
            topicAdmin.setUid(request.getUid());
            topicAdmin.setCreateTime(DateUtil.nowDateTime());
            topicAdmin.setTopicId(topic.getId());
            topicAdminService.save(topicAdmin);
        }
        //删除自己的管理员权限
        LambdaQueryWrapper<TopicAdminEntity> wrapper=new LambdaQueryWrapper<>();
        wrapper.eq(TopicAdminEntity::getUid,user.getUid());
        wrapper.eq(TopicAdminEntity::getTopicId,request.getTopicId());
        topicAdminService.remove(wrapper);
        //发消息通知变更用户
        String content = StrUtil.format(Constant.TOPIC_CHANGE, user.getUsername(), topic.getTopicName());
        messageService.sendMessageNotAsync(user.getUid(), request.getUid(), 0, Constant.PUSHARTICLE, content, Constant.TITLE_TOPIC);
        //更新圈主信息
        topic.setUid(request.getUid());
        return this.updateById(topic);
    }

    @Override
    @DSTransactional
    public void updateByAdmin(TopicEntity topic) {
        TopicEntity topicEntity = this.getById(topic.getId());
        if(!topicEntity.getUid().equals(topic.getUid())){
            AppUserEntity user = appUserService.getById(topic.getUid());
            if (null == user){
                throw new LinfengException("该圈主UID不存在");
            }
            //判断该用户是否为管理员
            Boolean isAdmin = topicAdminService.isAdmin(topic.getUid(), topic.getId());
            if(!isAdmin){
                TopicAdminEntity topicAdmin=new TopicAdminEntity();
                topicAdmin.setUid(topic.getUid());
                topicAdmin.setCreateTime(DateUtil.nowDateTime());
                topicAdmin.setTopicId(topic.getId());
                topicAdminService.save(topicAdmin);
            }
            //删除原本的管理员权限
            LambdaQueryWrapper<TopicAdminEntity> wrapper=new LambdaQueryWrapper<>();
            wrapper.eq(TopicAdminEntity::getUid,topicEntity.getUid());
            wrapper.eq(TopicAdminEntity::getTopicId,topic.getId());
            topicAdminService.remove(wrapper);
        }
        if(!topicEntity.getIsPrivacy().equals(topic.getIsPrivacy())){
            updatePostStatusBatch(topic.getIsPrivacy(),topicEntity.getId());
        }
        this.updateById(topic);
    }

    @Override
    public List<TopicEntity> getJoinTopicList(Integer id) {
        List<UserTopicEntity> list = userTopicService.lambdaQuery()
                .eq(UserTopicEntity::getUid, id)
                .list();
        List<Integer> idList = list.stream().map(UserTopicEntity::getTopicId).collect(Collectors.toList());
        return this.lambdaQuery()
                .eq(TopicEntity::getStatus, Constant.NORMAL)
                .in(TopicEntity::getId, idList)
                .list();
    }


    @Override
    @DSTransactional
    public void doBlock(BlockForm param, AppUserEntity user) {
        if(param.getUid().equals(user.getUid())){
            throw new LinfengException("不能拉黑自己");
        }
        TopicEntity topic = this.getById(param.getTopicId());
        Boolean isAdmin = topicAdminService.isAdmin(user.getUid(), param.getTopicId());
        if(!isAdmin && !topic.getUid().equals(user.getUid())){
            throw new LinfengException("你不是圈子管理员");
        }
        //判断拉黑对象是否合法
        Boolean admin = topicAdminService.isAdmin(param.getUid(), param.getTopicId());
        if(admin){
            if(!topic.getUid().equals(user.getUid())){
                throw new LinfengException("你不是圈主无法拉黑圈子管理员");
            }
        }

        Boolean isBlock = topicBlockService.isBlock(param.getTopicId(), param.getUid());
        if(isBlock){
            throw new LinfengException("请勿重复拉黑");
        }
        userTopicService.remove(new LambdaQueryWrapper<UserTopicEntity>()
                .eq(UserTopicEntity::getTopicId,topic.getId())
                .eq(UserTopicEntity::getUid,param.getUid()));
        topicAdminService.remove(new LambdaQueryWrapper<TopicAdminEntity>()
                .eq(TopicAdminEntity::getTopicId,param.getTopicId())
                .eq(TopicAdminEntity::getUid,param.getUid()));
        topicBlockService.addBlock(param.getTopicId(),param.getUid(),user.getUid());
        //圈子人数更新
        topic.setUserNum(topic.getUserNum()-1);
        this.updateById(topic);
    }

    @Override
    public void removeBlock(BlockForm param, AppUserEntity user) {
        TopicEntity topic = this.getById(param.getTopicId());
        Boolean isAdmin = topicAdminService.isAdmin(user.getUid(), param.getTopicId());
        if(!isAdmin && !topic.getUid().equals(user.getUid())){
            throw new LinfengException("你不是圈子管理员");
        }
        topicBlockService.removeBlock(param.getUid(),param.getTopicId());
    }

    @Override
    public void joinTopicApply(UserJoinTopicApplyForm request, AppUserEntity user) {
        Boolean isApply = topicApplyService.getApplyInfoByUserId(user.getUid(), request.getTopicId());
        if(isApply){
            throw new LinfengException("请勿重复提交申请");
        }
        Date date = DateUtil.nowDateTime();
        TopicApplyEntity apply=new TopicApplyEntity();
        apply.setTopicId(request.getTopicId());
        apply.setAnswer(request.getAnswer());
        apply.setQuestion(request.getQuestion());
        apply.setUid(user.getUid());
        apply.setStatus(Constant.NORMAL);
        apply.setCreateTime(date);
        apply.setUpdateTime(date);
        boolean save = topicApplyService.save(apply);
        if(!save){
            throw new LinfengException("提交申请失败");
        }
    }

    /**
     * 根据id查询圈子名称
     * @param topicIdList
     * @return
     */
    @Override
    public Map<Integer, String> getAllByList(List<Integer> topicIdList) {
        List<TopicEntity> topicList = this.lambdaQuery().in(TopicEntity::getId, topicIdList).list();
        List<TopicNameBatchResponse> list=new ArrayList<>();
        topicList.forEach(item->{
            TopicNameBatchResponse response=new TopicNameBatchResponse();
            BeanUtils.copyProperties(item,response);
            list.add(response);
        });
        return list.stream().collect(Collectors.toMap(TopicNameBatchResponse::getId,TopicNameBatchResponse::getTopicName));
    }

    @Override
    @DSTransactional
    public void saveTopicByAdmin(TopicEntity topic) {
        AppUserEntity user = appUserService.getById(topic.getUid());
        if(user==null){
            throw new LinfengException("用户id不存在");
        }
        topic.setCreateTime(DateUtil.nowDateTime());
        boolean save = this.save(topic);
        if(save) {
            UserTopicEntity userTopic=new UserTopicEntity();
            userTopic.setUid(user.getUid());
            userTopic.setTopicId(topic.getId());
            userTopic.setCreateTime(DateUtil.nowDateTime());
            userTopicService.save(userTopic);
            //圈子管理员表添加记录
            TopicAdminEntity topicAdmin =new TopicAdminEntity();
            topicAdmin.setCreateTime(DateUtil.nowDateTime());
            topicAdmin.setTopicId(topic.getId());
            topicAdmin.setUid(user.getUid());
            topicAdminService.save(topicAdmin);
        }
    }

    @Override
    public boolean privateCirclesOpen() {
        String value = configService.getValue(Constant.PRIVATE_CIRCLES_OPEN);
        if(value.equals("1")){
            String needVip = configService.getValue(Constant.PRIVATE_CIRCLES_NEED_VIP);
            if(needVip.equals("0")){
                return true;
            }else{
                AppUserEntity user = localUser.getUser();
                return user.getVip().equals(Constant.VIP_USER);
            }
        }
        return false;
    }

    /**
     * 批量更新帖子状态
     * @param value 私密状态
     * @param topicId 圈子ID
     */
    private void updatePostStatusBatch(Integer value,Integer topicId){
        boolean update = postService.lambdaUpdate()
                .set(PostEntity::getIsPrivate, value)
                .eq(PostEntity::getTopicId, topicId)
                .update();
//        if(!update){
//            throw new LinfengException("批量更新帖子状态失败");
//        }
    }

    /**
     * 检查用户创建圈子权限
     * @param user
     */
    private void checkAddTopicAuth(AppUserEntity user){
        String vipTopicCount = configService.getValue(Constant.VIP_TOPIC_NUMBER);
        String commonTopicCount = configService.getValue(Constant.COMMON_TOPIC_NUMBER);
        AppUserEntity appUser = appUserService.vipExpirationCheck(user);
        Integer count = Math.toIntExact(this.lambdaQuery().eq(TopicEntity::getUid, user.getUid()).count());
        if(appUser.getVip().equals(Constant.VIP_USER)){
            if(count>=Integer.valueOf(vipTopicCount)){
                throw new LinfengException("会员创建圈子数已达上限");
            }
        }else{
            if(count>=Integer.valueOf(commonTopicCount)){
                throw new LinfengException("普通用户创建圈子数已达上限");
            }
        }
    }

    /**
     * 组装帖子返回数据集合
     * @param page
     * @param queryWrapper
     * @return
     */
    public AppPageUtils  mapTopicList(Page<TopicEntity> page,QueryWrapper<TopicEntity> queryWrapper){

        Page<TopicEntity> page1 = this.page(page, queryWrapper);
        AppPageUtils pages=new AppPageUtils(page1);
        if(page1.getRecords().isEmpty()){
            return pages;
        }
        List<TopicEntity> list = (List<TopicEntity>) pages.getData();
        pages.setData(toTopicListResponseList(list));
        return pages;
    }

    private boolean isOfficialTopic(TopicEntity topicEntity) {
        return ObjectUtil.isNotNull(topicEntity)
                && ObjectUtil.isNotNull(topicEntity.getChannelType())
                && topicEntity.getChannelType() == 1;
    }

    private void applyTopicDisplayOrder(QueryWrapper<TopicEntity> queryWrapper) {
        queryWrapper.lambda()
                .orderByAsc(TopicEntity::getChannelType)
                .orderByDesc(TopicEntity::getTopType)
                .orderByDesc(TopicEntity::getUserNum)
                .orderByDesc(TopicEntity::getId);
    }

    private void applyTopicDisplayOrder(LambdaQueryWrapper<TopicEntity> queryWrapper) {
        queryWrapper.orderByAsc(TopicEntity::getChannelType)
                .orderByDesc(TopicEntity::getTopType)
                .orderByDesc(TopicEntity::getUserNum)
                .orderByDesc(TopicEntity::getId);
    }

    private List<TopicListResponse> toTopicListResponseList(List<TopicEntity> list) {
        if (ObjectUtil.isEmpty(list)) {
            return new ArrayList<>();
        }
        List<Integer> topicIdList = list.stream().map(TopicEntity::getId).collect(Collectors.toList());
        List<TopicPostResponse> countBatch = postService.findTopicPostCountBatch(topicIdList);
        Map<Integer, Integer> countMap = countBatch.stream().collect(Collectors.toMap(TopicPostResponse::getTopicId, TopicPostResponse::getNumber));
        List<Integer> ownerUidList = list.stream()
                .map(this::resolveTopicOwnerUid)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, AppUserEntity> ownerUserMap = new HashMap<>();
        if (!ownerUidList.isEmpty()) {
            List<AppUserEntity> ownerUserList = appUserService.getBatchUser(ownerUidList);
            if (ObjectUtil.isNotEmpty(ownerUserList)) {
                ownerUserMap.putAll(ownerUserList.stream().collect(Collectors.toMap(AppUserEntity::getUid, item -> item, (left, right) -> left)));
            }
        }
        AppUserEntity defaultOwnerUser = ownerUserMap.get(DEFAULT_TOPIC_OWNER_UID);
        if (ObjectUtil.isNull(defaultOwnerUser)) {
            defaultOwnerUser = appUserService.getById(DEFAULT_TOPIC_OWNER_UID);
            if (ObjectUtil.isNotNull(defaultOwnerUser)) {
                ownerUserMap.put(DEFAULT_TOPIC_OWNER_UID, defaultOwnerUser);
            }
        }
        final AppUserEntity resolvedDefaultOwnerUser = defaultOwnerUser;
        List<TopicListResponse> responseList = new ArrayList<>();
        list.forEach(topicEntity -> {
            TopicListResponse topicListResponse = new TopicListResponse();
            BeanUtils.copyProperties(topicEntity, topicListResponse);
            topicListResponse.setPostCount(ObjectUtil.defaultIfNull(countMap.get(topicEntity.getId()), 0));
            topicListResponse.setUserCount(ObjectUtil.defaultIfNull(topicListResponse.getUserNum(), 0));
            topicListResponse.setIsOfficial(isOfficialTopic(topicEntity));
            topicListResponse.setHongniangInfo(buildHongniangTopicInfo(topicEntity));
            AppUserEntity ownerUser = resolveTopicOwnerUser(topicEntity, ownerUserMap, resolvedDefaultOwnerUser);
            topicListResponse.setUid(resolveTopicOwnerUid(topicEntity, ownerUser));
            topicListResponse.setUserInfo(ownerUser);
            responseList.add(topicListResponse);
        });
        return responseList;
    }

    private AppUserShortInfoResponse buildTopicOwnerShortInfo(TopicEntity topicEntity, AppUserEntity ownerUser) {
        AppUserShortInfoResponse shortInfoResponse = new AppUserShortInfoResponse();
        if (ObjectUtil.isNotNull(ownerUser)) {
            BeanUtils.copyProperties(ownerUser, shortInfoResponse);
        } else {
            shortInfoResponse.setUid(resolveTopicOwnerUid(topicEntity));
        }
        return shortInfoResponse;
    }

    private Integer resolveTopicOwnerUid(TopicEntity topicEntity) {
        return resolveTopicOwnerUid(topicEntity, null);
    }

    private Integer resolveTopicOwnerUid(TopicEntity topicEntity, AppUserEntity ownerUser) {
        if (ObjectUtil.isNotNull(ownerUser) && ObjectUtil.isNotNull(ownerUser.getUid()) && ownerUser.getUid() > 0) {
            return ownerUser.getUid();
        }
        if (ObjectUtil.isNotNull(topicEntity)
                && ObjectUtil.isNotNull(topicEntity.getUid())
                && topicEntity.getUid() > 0) {
            return topicEntity.getUid();
        }
        return DEFAULT_TOPIC_OWNER_UID;
    }

    private AppUserEntity resolveTopicOwnerUser(TopicEntity topicEntity) {
        return resolveTopicOwnerUser(topicEntity, null, null);
    }

    private AppUserEntity resolveTopicOwnerUser(TopicEntity topicEntity,
                                                Map<Integer, AppUserEntity> ownerUserMap,
                                                AppUserEntity defaultOwnerUser) {
        Integer ownerUid = resolveTopicOwnerUid(topicEntity);
        AppUserEntity ownerUser = ownerUserMap == null ? null : ownerUserMap.get(ownerUid);
        if (ObjectUtil.isNull(ownerUser) && ownerUserMap == null) {
            ownerUser = appUserService.getById(ownerUid);
        }
        if (ObjectUtil.isNotNull(ownerUser)) {
            return ownerUser;
        }
        if (ObjectUtil.isNotNull(defaultOwnerUser)) {
            return defaultOwnerUser;
        }
        if (!DEFAULT_TOPIC_OWNER_UID.equals(ownerUid)) {
            return appUserService.getById(DEFAULT_TOPIC_OWNER_UID);
        }
        return null;
    }

    private HongniangTopicInfoResponse buildHongniangTopicInfo(TopicEntity topicEntity) {
        if (ObjectUtil.isNull(topicEntity)
                || !Integer.valueOf(2).equals(topicEntity.getChannelType())) {
            return null;
        }
        if (ObjectUtil.isNull(topicEntity.getHongniangId()) || topicEntity.getHongniangId() <= 0) {
            return null;
        }
        HongniangInfoEntity hongniang = hongniangService.getById(topicEntity.getHongniangId());
        if (ObjectUtil.isNull(hongniang)) {
            return null;
        }
        HongniangTopicInfoResponse response = new HongniangTopicInfoResponse();
        BeanUtils.copyProperties(hongniang, response);
        return response;
    }

    private Integer resolveHongniangId(AppUserEntity user) {
        if (ObjectUtil.isNull(user)) {
            return null;
        }
        Integer hongniangId = user.getHongniangId();
        if (hongniangId == null || hongniangId <= 0) {
            HongniangInfoEntity hongniang = hongniangService.getByUserId(user.getUid());
            if (hongniang != null) {
                hongniangId = hongniang.getId();
            }
        }
        return hongniangId;
    }


}
