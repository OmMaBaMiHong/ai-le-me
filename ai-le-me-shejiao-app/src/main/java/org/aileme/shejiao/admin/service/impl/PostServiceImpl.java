/**
 * -----------------------------------
 * Copyright (c) 2021-2023
 * All rights reserved, Designed By my.hots.love
 * <p>
 * 商业版授权联系技术客服	 QQ:  3582996245
 * 严禁分享、盗用、转卖源码或非法牟利！
 * 版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.admin.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import java.time.Duration;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import cn.dev33.satoken.stp.StpUtil;
import org.aileme.common.core.exception.ServiceException;
import org.aileme.common.oss.core.OssClient;
import org.aileme.common.oss.entity.UploadResult;
import org.aileme.common.oss.factory.OssFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.admin.dao.PostDao;
import org.aileme.shejiao.admin.utils.LocalUser;
import org.aileme.shejiao.admin.utils.WechatUtil;
import org.aileme.shejiao.api.service.*;
import org.aileme.shejiao.common.enums.BillDetailEnum;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.*;
import org.aileme.shejiao.common.config.RestTemplateUtil;
import org.aileme.shejiao.domain.entity.admin.*;
import org.aileme.shejiao.domain.entity.app.*;
import org.aileme.shejiao.domain.entity.sys.SysArea;
import org.aileme.shejiao.domain.entity.sys.SysUniversity;
import org.aileme.shejiao.domain.enums.CommentStatus;
import org.aileme.shejiao.domain.enums.GenderStatus;
import org.aileme.shejiao.domain.enums.PrivacyStatus;
import org.aileme.shejiao.domain.enums.UserTypeStatus;
import org.redisson.client.codec.StringCodec;
import org.aileme.shejiao.domain.param.app.*;
import org.aileme.shejiao.domain.vo.*;
import org.aileme.shejiao.app.oss.factory.OSSFactory;


import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@DS("master")
@Service("postService")
public class PostServiceImpl extends ServiceImpl<PostDao, PostEntity> implements PostService {

    @Autowired
    private AppUserService appUserService;
    @Lazy
    @Autowired
    private TopicService topicService;
    @Autowired
    private FollowService followService;
    @Autowired
    private PostCollectionService postCollectionService;
    @Autowired
    private PostFabulousService postFabulousService;
    @Autowired
    private UserTopicService userTopicService;
    @Autowired
    private CommentService commentService;
    @Autowired
    private PostTagService postTagService;
    @Autowired
    private TagsService tagsService;
    @Lazy
    @Autowired
    private DiscussService discussService;
    @Autowired
    private MessageService messageService;
    @Autowired
    private SensitiveService sensitiveService;
    @Autowired
    private TopicAdminService topicAdminService;
    @Autowired
    private LocalUser localUser;
    @Autowired
    private BillService billService;
    @Autowired
    private PostDao postDao;
    @Autowired
    private VoteSubjectService voteSubjectService;
    @Autowired
    private VoteOptionService voteOptionService;
    @Autowired
    private VoteResultService voteResultService;
    @Autowired
    private SysConfigService configService;
    @Autowired
    private WechatRuntimeConfigService wechatRuntimeConfigService;
    @Autowired
    private RestTemplateUtil restTemplateUtil;
    @Autowired
    private TopicTopService topicTopService;
    @Autowired
    private CommentThumbsService commentThumbsService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private UserLevelService userLevelService;
    @Autowired
    private SysAreaService sysAreaService;
    @Autowired
    private SysUniversityService sysUniversityService;
    @Autowired
    private UserVideoService userVideoService;
    @Autowired
    private XiangqinActivityService xiangqinActivityService;
    @Autowired
    private XiangqinEnrollmentService xiangqinEnrollmentService;
    @Autowired
    private HongniangService hongniangService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<PostEntity> queryWrapper = new QueryWrapper<>();
        //条件查询
        String key = (String) params.get("key");
        String status = (String) params.get("status");
        String type = (String) params.get("type");
        if (!WechatUtil.isEmpty(key)) {
            if (NumberUtil.isInteger(key)) {
                queryWrapper.lambda().eq(PostEntity::getId, key);
            } else {
                queryWrapper.like("content", key).or().like("title", key);
            }

        }
        if (!WechatUtil.isEmpty(status)) {
            queryWrapper.eq("status", Integer.parseInt(status));
        }
        if (!WechatUtil.isEmpty(type)) {
            queryWrapper.eq("type", Integer.parseInt(type));
        }
        queryWrapper.lambda().orderByDesc(PostEntity::getId);
        IPage<PostEntity> page = this.page(
                new Query<PostEntity>().getPage(params),
                queryWrapper
        );
        List<PostEntity> data = page.getRecords();

        List<AdminPostListResponse> responseList = new ArrayList<>();
        data.forEach(l -> {
            AdminPostListResponse response = new AdminPostListResponse();
            BeanUtils.copyProperties(l, response);
            if (ObjectUtil.isNotNull(response.getDiscussId()) && response.getDiscussId() > 0) {
                DiscussEntity discussEntity = discussService.getById(response.getDiscussId());
                if (ObjectUtil.isNotNull(discussEntity)) {
                    response.setDiscussTitle(discussEntity.getTitle());
                }
            }
            response.setTopicName(topicService.getById(response.getTopicId()).getTopicName());
            response.setCollectionCount(postCollectionService.collectCount(response.getId()));
            response.setCommentCount(commentService.getCountByPostId(response.getId()));
            AppUserShortInfoResponse shortInfoResponse = new AppUserShortInfoResponse();
            BeanUtils.copyProperties(appUserService.getById(response.getUid()), shortInfoResponse);
            response.setUserInfo(shortInfoResponse);
            response.setAvatar(shortInfoResponse.getAvatar());
            response.setMediasList(JsonUtils.JsonToList(l.getMedia()));
            responseList.add(response);
        });
        PageUtils pageUtils = new PageUtils(page);
        pageUtils.setList(responseList);
        return pageUtils;
    }

    @Override
    public Integer findTopicPostCount(Integer topicId) {
        LambdaQueryWrapper<PostEntity> lambdaQueryWrapper = Wrappers.lambdaQuery();
        lambdaQueryWrapper.eq(PostEntity::getTopicId, topicId);
        lambdaQueryWrapper.eq(PostEntity::getStatus, 0);
        return Math.toIntExact(baseMapper.selectCount(lambdaQueryWrapper));
    }

    @Override
    public List<TopicPostResponse> findTopicPostCountBatch(List<Integer> topicIdList) {
        return postDao.findTopicPostCountBatch(topicIdList);
    }

    @Override
    public PostDetailResponse detail(Integer id) {
        PostEntity post = this.getById(id);
        if (ObjectUtil.isNull(post)) {
            throw new LinfengException("该帖子不存在或已删除");
        }
        if (Objects.equals(post.getStatus(), Constant.POST_REVIEWED)) {
            throw new LinfengException("该帖子待审核");
        }
        if (Objects.equals(post.getStatus(), Constant.POST_BANNER)) {
            throw new LinfengException("该帖子已下架");
        }
        TopicEntity topicEntity = topicService.getById(post.getTopicId());
        //如果不存在该圈子则删除帖子
        if (topicEntity == null) {
            this.removeById(id);
            throw new LinfengException("该帖子不存在");
        }
        if (topicEntity.getStatus().equals(Constant.BAN)) {
            this.removeById(id);
            throw new LinfengException("帖子所在圈子已禁用");
        }
        AppUserEntity user = localUser.getUser();
        if (Objects.equals(post.getCut(), 1)) {
            if (ObjectUtil.isNull(user)) {
                throw new LinfengException("付费帖请先登录");
            }
            //查看是否是帖子作者本人
            if (!post.getUid().equals(user.getUid())) {
                boolean isPay = billService.vipPostIsPay(id, user.getUid());
                if (!isPay) {
                    throw new LinfengException("付费帖请先支付", 999);
                }
            }
        }
        if (Objects.equals(post.getIsPrivate(), PrivacyStatus.PRIVACY.getValue())) {
            if (ObjectUtil.isNotNull(user)) {
                Boolean isJoin = userTopicService.isJoin(user.getUid(), post.getTopicId());
                if (!isJoin) {
                    throw new LinfengException("私密贴暂无查看权限");
                }
            } else {
                throw new LinfengException("私密贴暂无查看权限");
            }
        }
        post.setReadCount(post.getReadCount() + 1);
        baseMapper.updateById(post);
        PostDetailResponse response = new PostDetailResponse();
        BeanUtils.copyProperties(post, response);
        AppUserEntity userInfo = appUserService.getById(post.getUid());
//        if(!WechatUtil.isEmpty(userInfo.getMobile())){
//            userInfo.setMobile(WechatUtil.maskMobile(userInfo.getMobile()));
//        }
//        response.setUserInfo(userInfo);
        AppUserShortInfoResponse userShortInfoResponse = new AppUserShortInfoResponse();
        BeanUtils.copyProperties(userInfo, userShortInfoResponse);
        response.setUserInfo(userShortInfoResponse);
        if (ObjectUtil.isNotNull(post.getDiscussId()) && post.getDiscussId() > 0) {
            response.setDiscussName(discussService.getDiscussNameById(post.getDiscussId()));
        }
        if (ObjectUtil.isNotNull(post.getActivityId()) && post.getActivityId() > 0) {
            XiangqinActivityEntity activity = xiangqinActivityService.getById(post.getActivityId());
            if (activity != null) {
                response.setActivityId(activity.getId());
                response.setActivityTitle(activity.getTitle());
                response.setActivityStatus(activity.getStatus());
            }
        }
        TopicDetailResponse topicDetailResponse = new TopicDetailResponse();
        BeanUtils.copyProperties(topicEntity, topicDetailResponse);
        Integer topicId = topicEntity.getId();
        Integer postCount = this.findTopicPostCount(topicId);
        topicDetailResponse.setPostCount(postCount);
//        Integer userCount=userTopicService.findUserTopicService(topicId);
        topicDetailResponse.setUserCount(topicEntity.getUserNum());
        response.setTopicInfo(topicDetailResponse);

        if (ObjectUtil.isNull(user)) {
            response.setIsFollow(false);
            response.setIsCollection(false);
            response.setIsLike(false);
            response.setIsThumb(false);
        } else {
            response.setIsFollow(followService.isFollowOrNot(user.getUid(), post.getUid()));
            boolean liked = postCollectionService.isCollection(user.getUid(), post.getId());
            response.setIsCollection(liked);
            response.setIsLike(liked);
            response.setIsThumb(postFabulousService.isThumb(user.getUid(), post.getId()));
        }
        int likeCount = postCollectionService.collectCount(post.getId());
        response.setCollectionCount(likeCount);
        response.setLikeCount(likeCount);
        response.setCommentCount(commentService.getCountByPostId(post.getId()));
        response.setMedia(JsonUtils.JsonToList(post.getMedia()));//文件处理
        if (post.getVideoId() != null) {
            UserVideoEntity userVideo = userVideoService.getPlayableVideoById(post.getVideoId());
            if (userVideo != null) {
                response.setCoverUrl(JsonUtils.normalizeUrlValue(userVideo.getCoverUrl()));
                if (StrUtil.isNotBlank(userVideo.getVideoUrl())) {
                    response.setMedia(Collections.singletonList(JsonUtils.normalizeUrlValue(userVideo.getVideoUrl())));
                }
            }
        }

        if (Objects.equals(post.getType(), Constant.POST_TYPE_VIDEO)) {
            //视频源加密
            String encryptUrl = UrlEncryptor.encryptUrl(post.getMedia());
            List<String> list = new ArrayList<>();
            list.add(encryptUrl);
            response.setMedia(list);
        } else {
            response.setMedia(JsonUtils.JsonToList(post.getMedia()));//文件处理
        }

        String showType = configService.getValue(Constant.POST_DETAIL_SHOW_TYPE);
        String rewardBtn = configService.getValue(Constant.REWARD_BTN);
        response.setShowType(showType);
        response.setRewardBtn(rewardBtn);
        //投票
        if (post.getVoteId() != null && post.getVoteId() > 0) {
            VoteSubjectEntity voteSubject = voteSubjectService.lambdaQuery().eq(VoteSubjectEntity::getId, post.getVoteId()).one();
            List<VoteOptionEntity> voteOptionList = voteOptionService.lambdaQuery().eq(VoteOptionEntity::getVoteId, post.getVoteId()).list();
            VoteInfoResponse voteInfoResponse = new VoteInfoResponse();
            BeanUtils.copyProperties(voteSubject, voteInfoResponse);
            voteInfoResponse.setOptions(voteOptionList);
            voteInfoResponse.setExpireTime(DateUtil.getSecondTimestamp(voteSubject.getExpireTime()));
            voteInfoResponse.setTime(voteSubject.getExpireTime());
            response.setVoteInfo(voteInfoResponse);
            if (ObjectUtil.isNull(user)) {
                response.setIsVoteResult(false);
            } else {
                Long voteResultCount = voteResultService.lambdaQuery()
                        .eq(VoteResultEntity::getVoteId, post.getVoteId())
                        .eq(VoteResultEntity::getUid, user.getUid())
                        .count();
                response.setIsVoteResult(ObjectUtil.isNotNull(voteResultCount) && voteResultCount > 0);
            }

        }
        return response;
    }

    @Override
    public AppPageUtils queryPageList(PostListForm request) {
        AppPageUtils appPage;
        Page<PostEntity> page = new Page<>(request.getPage(), 10);
        QueryWrapper<PostEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PostEntity::getStatus, Constant.POST_NORMAL);
        if (ObjectUtil.isNotNull(request.getDisId())) {
            Integer uid = 0;
            AppUserEntity user = localUser.getUser();
            if (ObjectUtil.isNotNull(user)) {
                uid = user.getUid();
            }
            queryWrapper.lambda().eq(PostEntity::getDiscussId, request.getDisId());
            if (ObjectUtil.isNotNull(request.getSort()) && request.getSort().equals("hot")) {
                queryWrapper.lambda().orderByDesc(PostEntity::getReadCount);
            } else {
                queryWrapper.lambda().orderByDesc(PostEntity::getId);
            }
            appPage = this.mapPostList(page, queryWrapper, uid);
        } else {
            if (ObjectUtil.isNotNull(request.getTopicId())) {
                queryWrapper.lambda().eq(PostEntity::getTopicId, request.getTopicId());
            }
            if (ObjectUtil.isNotNull(request.getActivityId()) && request.getActivityId() > 0) {
                queryWrapper.lambda().eq(PostEntity::getActivityId, request.getActivityId());
            }
            if (ObjectUtil.isNotNull(request.getOrder())) {
                if (request.getOrder().equals(Constant.ORDER_DESC_READCOUNT)) {
                    queryWrapper.lambda().orderByDesc(PostEntity::getReadCount);
                } else if (request.getOrder().equals(Constant.ORDER_DESC_ID)) {
                    queryWrapper.lambda().orderByDesc(PostEntity::getId);
                } else if (request.getOrder().equals(Constant.ORDER_TYPE_ONE_TWO)) {
                    queryWrapper.lambda()
                            .and(wrapper -> wrapper.eq(PostEntity::getType, Constant.POST_TYPE_TEXT).or()
                                    .eq(PostEntity::getType, Constant.POST_TYPE_VIDEO));
                    queryWrapper.lambda().orderByDesc(PostEntity::getId);
                } else if (request.getOrder().equals(Constant.ORDER_TYPE_FOUR)) {
                    queryWrapper.lambda().eq(PostEntity::getType, Constant.POST_TYPE_VOTE);
                    queryWrapper.lambda().orderByDesc(PostEntity::getId);
                } else if (request.getOrder().equals(Constant.ORDER_TYPE_THREE)) {
                    queryWrapper.lambda().eq(PostEntity::getType, Constant.POST_TYPE_ARTICLE);
                    queryWrapper.lambda().orderByDesc(PostEntity::getId);
                }
            } else {
                queryWrapper.orderByDesc("post_top", "id");
            }

            if (ObjectUtil.isNotNull(request.getUid())) {
                queryWrapper.lambda().eq(PostEntity::getUid, request.getUid());
                if (ObjectUtil.isNotNull(request.getMyUid()) && request.getMyUid() != 0) {
                    appPage = this.mapPostList(page, queryWrapper, request.getMyUid());
                } else {
                    appPage = this.mapPostList(page, queryWrapper, request.getUid());
                }
            } else {
                AppUserEntity user = localUser.getUser();
                if (ObjectUtil.isNull(user)) {
                    appPage = this.mapPostList(page, queryWrapper, 0);
                } else {
                    appPage = this.mapPostList(page, queryWrapper, user.getUid());
                }
            }
        }
        return appPage;
    }

    /**
     * 根据话题ID查询帖子
     * @param request
     * @return
     */
    @Override
    public AppPageUtils getPostListByDiscussId(PostListForm request) {
        AppPageUtils appPage = null;
        Page<PostEntity> page = new Page<>(request.getPage(), 10);
        QueryWrapper<PostEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PostEntity::getStatus, Constant.POST_NORMAL);
        if (ObjectUtil.isNotNull(request.getDisId())) {
            Integer uid = 0;
            AppUserEntity user = localUser.getUser();
            if (ObjectUtil.isNotNull(user)) {
                uid = user.getUid();
                //处理私密圈帖子
                DiscussEntity discuss = discussService.getById(request.getDisId());
                if (discuss == null) {
                    throw new LinfengException("话题不存在");
                }
                TopicEntity topic = topicService.getById(discuss.getTopicId());
                if (topic.getIsPrivacy().equals(PrivacyStatus.PRIVACY.getValue())) {
                    if (!userTopicService.isJoin(user.getUid(), discuss.getTopicId())) {
                        queryWrapper.lambda().eq(PostEntity::getIsPrivate, PrivacyStatus.PUBLIC.getValue());
                    }
                }
            } else {
                queryWrapper.lambda().eq(PostEntity::getIsPrivate, PrivacyStatus.PUBLIC.getValue());
            }

            queryWrapper.lambda().eq(PostEntity::getDiscussId, request.getDisId());
            if (ObjectUtil.isNotNull(request.getSort()) && request.getSort().equals("hot")) {
                queryWrapper.lambda().orderByDesc(PostEntity::getReadCount);
            } else {
                queryWrapper.lambda().orderByDesc(PostEntity::getId);
            }
            appPage = this.mapPostList(page, queryWrapper, uid);
        }
        return appPage;
    }

    /**
     * 查询圈子内的帖子
     * @param request
     * @return
     */
    @Override
    public AppPageUtils getListByTopicId(PostListForm request) {
        AppPageUtils appPage;
        Page<PostEntity> page = new Page<>(request.getPage(), 10);
        QueryWrapper<PostEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PostEntity::getStatus, Constant.POST_NORMAL);

        if (ObjectUtil.isNotNull(request.getTopicId())) {
            queryWrapper.lambda().eq(PostEntity::getTopicId, request.getTopicId());
        }
        //查询私密圈帖子逻辑
        AppUserEntity user = localUser.getUser();
        if (ObjectUtil.isNotNull(user)) {
            TopicEntity topic = topicService.getById(request.getTopicId());
            if (topic.getIsPrivacy().equals(PrivacyStatus.PRIVACY.getValue())) {
                if (!userTopicService.isJoin(user.getUid(), request.getTopicId())) {
                    queryWrapper.lambda().eq(PostEntity::getIsPrivate, PrivacyStatus.PUBLIC.getValue());
                }
            }
        } else {
            queryWrapper.lambda().eq(PostEntity::getIsPrivate, PrivacyStatus.PUBLIC.getValue());
        }
        if (ObjectUtil.isNotNull(request.getOrder())) {
            if (request.getOrder().equals(Constant.ORDER_DESC_READCOUNT)) {
                queryWrapper.lambda().orderByDesc(PostEntity::getReadCount);
            } else if (request.getOrder().equals(Constant.ORDER_DESC_ID)) {
                queryWrapper.lambda().orderByDesc(PostEntity::getId);
            } else if (request.getOrder().equals(Constant.ORDER_TYPE_ONE_TWO)) {
                queryWrapper.lambda()
                        .and(wrapper -> wrapper.eq(PostEntity::getType, Constant.POST_TYPE_TEXT).or()
                                .eq(PostEntity::getType, Constant.POST_TYPE_VIDEO));
                queryWrapper.lambda().orderByDesc(PostEntity::getId);
            } else if (request.getOrder().equals(Constant.ORDER_TYPE_FOUR)) {
                queryWrapper.lambda().eq(PostEntity::getType, Constant.POST_TYPE_VOTE);
                queryWrapper.lambda().orderByDesc(PostEntity::getId);
            } else if (request.getOrder().equals(Constant.ORDER_TYPE_THREE)) {
                queryWrapper.lambda().eq(PostEntity::getType, Constant.POST_TYPE_ARTICLE);
                queryWrapper.lambda().orderByDesc(PostEntity::getId);
            }
        } else {
            queryWrapper.orderByDesc("post_top", "id");
        }
        if (ObjectUtil.isNull(user)) {
            appPage = this.mapPostList(page, queryWrapper, 0);
        } else {
            appPage = this.mapPostList(page, queryWrapper, user.getUid());
        }


        return appPage;
    }

    @Override
    public AppPageUtils getListByUid(PostListForm request) {
        AppPageUtils appPage = null;
        Page<PostEntity> page = new Page<>(request.getPage(), 10);
        QueryWrapper<PostEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PostEntity::getStatus, Constant.POST_NORMAL);


        if (ObjectUtil.isNotNull(request.getUid())) {
            queryWrapper.lambda().eq(PostEntity::getUid, request.getUid());
            if (ObjectUtil.isNull(request.getMyUid()) || !request.getUid().equals(request.getMyUid())) {
                queryWrapper.lambda().eq(PostEntity::getIsPrivate, PrivacyStatus.PUBLIC.getValue());
            }
            if (ObjectUtil.isNotNull(request.getMyUid()) && request.getMyUid() != 0) {
                appPage = this.mapPostList(page, queryWrapper, request.getMyUid());
            } else {
                appPage = this.mapPostList(page, queryWrapper, request.getUid());
            }
        }
        return appPage;
    }

    @Override
    @DSTransactional
    public Integer addComment(AddCommentForm request, AppUserEntity user) {
        if (!user.getStatus().equals(Constant.NORMAL)) {
            throw new LinfengException("您的账号已被禁用！");
        }
        Boolean examine = sensitiveService.checkContent(request.getContent());
        String value = configService.getValue(Constant.COMMENT_CHECK);
        CommentEntity commentEntity = new CommentEntity();
        BeanUtils.copyProperties(request, commentEntity);
        commentEntity.setCreateTime(DateUtil.nowDateTime());
        commentEntity.setUid(user.getUid());
        commentEntity.setMedia(request.getMedia() == null ? null : JSON.toJSONString(request.getMedia()));
        if (examine) {
            commentEntity.setStatus(CommentStatus.AUDIT.getValue());
        } else {
            if (value.equals("1")) {
                commentEntity.setStatus(CommentStatus.AUDIT.getValue());
            } else {
                commentEntity.setStatus(CommentStatus.NORMAL.getValue());
                //评论数量缓存修改
                org.aileme.common.redis.utils.RedisUtils.getClient().getMap(RedisKeys.getPostKey(request.getPostId()), StringCodec.INSTANCE).addAndGet(ConfigConstant.POST_COMMENT_NUM, 1);
            }
        }
        commentService.save(commentEntity);
        //清理评论列表缓存
        org.aileme.common.redis.utils.RedisUtils.deleteObject(ConfigConstant.COMMENT_KEY + request.getPostId());

        PostEntity post = this.getById(request.getPostId());
        //如果是评论动态则通知动态的作者 如果是回复评论则通知评论用户
        if (ObjectUtil.isNull(request.getToUid())) {
            String content = StrUtil.format(Constant.CONTENT_COMMENT, user.getUsername(), post.getTitle(), request.getContent());
            messageService.sendMessageNotAsync(user.getUid(), post.getUid(), request.getPostId(), Constant.COMMENT, content, Constant.TITLE_COMMENT);
        } else {
            String content = StrUtil.format(Constant.CONTENT_COMMENT_REPLY, user.getUsername(), post.getTitle(), request.getContent());
            messageService.sendMessageNotAsync(user.getUid(), request.getToUid(), request.getPostId(), Constant.COMMENT, content, Constant.TITLE_COMMENT);
        }
        if (value.equals("1")) {
            return 1;
        }
        return 0;
    }

    @Override
    public void addCollection(AddCollectionForm request, AppUserEntity user) {
        Boolean collection = postCollectionService.isCollection(user.getUid(), request.getId());
        if (collection) {
            throw new LinfengException("请勿重复点赞");
        }
        PostCollectionEntity pc = new PostCollectionEntity();
        pc.setPostId(request.getId());
        pc.setUid(user.getUid());
        boolean save = postCollectionService.save(pc);
        if (!save) {
            throw new LinfengException("点赞失败");
        }
        org.aileme.common.redis.utils.RedisUtils.getClient().getMap(RedisKeys.getPostKey(request.getId()), StringCodec.INSTANCE).addAndGet(ConfigConstant.POST_STAR_NUM, 1);
        PostEntity post = this.getById(request.getId());
        String content = StrUtil.format(Constant.CONTENT_POST_STAR, user.getUsername(), post.getTitle());
        messageService.sendMessage(user.getUid(), post.getUid(), request.getId(), Constant.COLLECT, content, Constant.TITLE_COLLECT);

    }

    @Override
    public AppPageUtils joinTopicPost(Integer currPage, AppUserEntity user) {
        List<UserTopicEntity> topicList = userTopicService.getTopicIdByUid(user.getUid());
        List<Integer> topicIdList = topicList.stream().map(UserTopicEntity::getTopicId).collect(Collectors.toList());
        if (topicIdList.isEmpty()) {
            return new AppPageUtils(null, 0, 20, 1);
        }
        Page<PostEntity> page = new Page<>(currPage, 10);
        QueryWrapper<PostEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PostEntity::getStatus, Constant.POST_NORMAL);
        queryWrapper.lambda().in(PostEntity::getTopicId, topicIdList);
        queryWrapper.orderByDesc("post_top", "id");
        return this.mapPostList(page, queryWrapper, user.getUid());
    }

    @Override
    public AppPageUtils lastPost(Integer currPage, Integer classId) {
        Page<PostEntity> page = new Page<>(currPage, 10);
        QueryWrapper<PostEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PostEntity::getStatus, Constant.POST_NORMAL);
        queryWrapper.lambda().eq(PostEntity::getIsPrivate, PrivacyStatus.PUBLIC.getValue());
        if (ObjectUtil.isNotNull(classId)) {
            List<Integer> topicIdList = topicService.lambdaQuery()
                    .select(TopicEntity::getId)
                    .eq(TopicEntity::getCateId, classId)
                    .eq(TopicEntity::getStatus, Constant.TOPIC_NORMAL)
                    .list()
                    .stream()
                    .map(TopicEntity::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (topicIdList.isEmpty()) {
                return new AppPageUtils(null, 0, 10, currPage);
            }
            queryWrapper.lambda().in(PostEntity::getTopicId, topicIdList);
        }
        queryWrapper.orderByDesc("post_top", "id");
        AppUserEntity user = localUser.getUser();
        if (ObjectUtil.isNull(user)) {
            return this.mapPostList(page, queryWrapper, 0);
        }
        return this.mapPostList(page, queryWrapper, user.getUid());
    }

    @Override
    public AppPageUtils followUserPost(Integer currPage, AppUserEntity user) {
        List<Integer> followUidList = followService.getFollowUids(user);
        if (followUidList.isEmpty()) {
            return null;
        }
        QueryWrapper<PostEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PostEntity::getStatus, Constant.POST_NORMAL);
        queryWrapper.lambda().eq(PostEntity::getIsPrivate, PrivacyStatus.PUBLIC.getValue());
        queryWrapper.lambda().in(PostEntity::getUid, followUidList);
        queryWrapper.orderByDesc("post_top", "id");
        Page<PostEntity> page = new Page<>(currPage, 10);
        return this.mapPostList(page, queryWrapper, user.getUid());
    }

    /**
     * 选取圈子中热度最高的三条动态的首图作为展示
     * 加缓存
     * @param id
     * @return
     */
    @Override
    public List<String> findThreeMedia(Integer id) {
        String result = org.aileme.common.redis.utils.RedisUtils.getCacheObject(ConfigConstant.TOPIC_IMAGE_KEY + id);
        if (!WechatUtil.isEmpty(result)) {
            return JSONObject.parseArray(result, String.class);
        }
        QueryWrapper<PostEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PostEntity::getTopicId, id);
        queryWrapper.lambda().eq(PostEntity::getType, Constant.POST_TYPE_TEXT);
        queryWrapper.lambda().eq(PostEntity::getStatus, Constant.POST_NORMAL);
        queryWrapper.lambda().orderByDesc(PostEntity::getReadCount);
        queryWrapper.last("limit 10");
        List<PostEntity> postEntityList = baseMapper.selectList(queryWrapper);
        List<String> imageList = new ArrayList<>();
        for (int i = 0; i < postEntityList.size(); i++) {
            if (!postEntityList.get(i).getMedia().equals("")) {
                List<String> jsonToList = JsonUtils.JsonToList(postEntityList.get(i).getMedia());
                if (jsonToList.size() > 0) {
                    if (imageList.size() > 2) {
                        break;
                    } else {
                        imageList.add(jsonToList.get(0));
                    }
                }
            }
        }
        org.aileme.common.redis.utils.RedisUtils.setCacheObject(ConfigConstant.TOPIC_IMAGE_KEY + id, JSON.toJSON(imageList).toString(), Duration.ofSeconds(60 * 60 * 6));
        return imageList;
    }

    @Override
    public Long getPostNumberByUid(Integer uid) {
        return this.lambdaQuery()
                .eq(PostEntity::getUid, uid)
                .and(wrapper -> wrapper.eq(PostEntity::getStatus, Constant.POST_NORMAL).or().eq(PostEntity::getStatus, Constant.POST_REVIEWED))
                .count();
    }

    @Override
    public Integer addPost(AddPostForm request, AppUserEntity appUser) {
        AppUserEntity user = appUserService.getById(appUser.getUid());
        if (user.getStatus().equals(Constant.BAN)) {
            throw new LinfengException("您的账号已被禁用");
        }
        //如果为视频类型帖子时检测是否上传视频
        if (request.getType().equals(Constant.POST_TYPE_VIDEO) && request.getMedia().isEmpty()) {
            throw new LinfengException("请上传视频后再发布");
        }
        //如果为付费类型帖子检查付费金额等
        if (request.getCut() == 1) {
            if (request.getPay().compareTo(BigDecimal.ZERO) < 0
                    || request.getPay().compareTo(new BigDecimal(100)) > 0) {
                throw new LinfengException("付费金额不合法");
            }
            if (WechatUtil.isEmpty(request.getBrief())) {
                throw new LinfengException("付费简介不能为空");
            }
            String isOpen = configService.getValue(Constant.PAY_POST_BTN);
            if (isOpen.equals("1")) {
                throw new LinfengException("付费功能未开启");
            }
            String isVipOpen = configService.getValue(Constant.PAY_POST_VIP);
            if (isVipOpen.equals("1") && user.getVip().equals(Constant.COMMON_USER)) {
                throw new LinfengException("您不是会员无付费贴权限");
            }
        }
        sensitiveService.checkPostContent(request.getContent());
        sensitiveService.checkPostContent(request.getTitle());
        validateActivityLinkPermission(request.getActivityId(), user);

        PostEntity post = new PostEntity();
        BeanUtils.copyProperties(request, post);
        post.setMedia(JSON.toJSONString(request.getMedia()));
        post.setUid(user.getUid());
        post.setCreateTime(DateUtil.nowDateTime());
        TopicEntity topic = topicService.getById(request.getTopicId());
        post.setIsPrivate(topic.getIsPrivacy());
        //查看是否要审核
        if (request.getCut() == 1) {
            String vipPost = configService.getValue(Constant.POST_VIP);
            if (vipPost.equals("0")) {
                post.setStatus(Constant.POST_REVIEWED);
            }
        } else {
            String normalPost = configService.getValue(Constant.NORMAL_POST);
            if (normalPost.equals("0")) {
                post.setStatus(Constant.POST_REVIEWED);
            }
        }
        if (this.save(post)) {
            // 保存话题关联
            if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
                postTagService.savePostTags(post.getId(), request.getTagIds());
                // 更新话题使用次数
                for (Integer tagId : request.getTagIds()) {
                    tagsService.increaseUsageCount(tagId);
                }
            }
            //发帖奖励积分
            rewardIntegralByPosting(user);
            return post.getId();
        }
        return 0;
    }

    @Override
    public AppPageUtils myCollectPost(Integer page, AppUserEntity user) {
        List<Integer> postIdList = postCollectionService.getPostListByUid(user.getUid());
        if (postIdList.size() == 0) {
            return new AppPageUtils(null, 0, 10, 1);
        }
        QueryWrapper<PostEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PostEntity::getStatus, Constant.POST_NORMAL);
        queryWrapper.lambda().in(PostEntity::getId, postIdList);
        queryWrapper.lambda().orderByDesc(PostEntity::getId);
        Page<PostEntity> pages = new Page<>(page, 10);
        return this.mapPostList(pages, queryWrapper, user.getUid());
    }

    @Override
    public AppPageUtils myPost(Integer page, AppUserEntity user) {
        QueryWrapper<PostEntity> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("status",Constant.POST_NORMAL);
        queryWrapper.lambda().in(PostEntity::getUid, user.getUid());
        queryWrapper.lambda().orderByDesc(PostEntity::getId);
        Page<PostEntity> pages = new Page<>(page, 10);
        return this.mapPostList(pages, queryWrapper, user.getUid());
    }

    @Override
    public AppPageUtils search(Integer page, String keyword) {
        if (page == 1) {
            if (localUser != null) {
                Integer uid = localUser.getUser().getUid();
                searchService.setSearchContent(keyword, uid);
            }
        }
        QueryWrapper<PostEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PostEntity::getStatus, Constant.POST_NORMAL);
        queryWrapper.lambda().eq(PostEntity::getIsPrivate, PrivacyStatus.PUBLIC.getValue());
        queryWrapper.and(qr -> qr.like("content", keyword).or().like("title", keyword));
        queryWrapper.orderByDesc("post_top", "id");
        Page<PostEntity> pages = new Page<>(page, 10);
        return this.mapPostList(pages, queryWrapper, 0);
    }

    /**
     * 用户端删帖
     * @param id
     * @param uid
     */
    @Override
    @DSTransactional
    public void del(Integer id, Integer uid) {
        PostEntity post = this.getById(id);
        if (ObjectUtil.isNull(post)) {
            throw new LinfengException("该帖子不存在");
        }
        if (!post.getUid().equals(uid)) {
            //圈子管理员可以删除帖子
            Boolean admin = topicAdminService.isAdmin(uid, post.getTopicId());
            if (!admin) {
                throw new LinfengException("不能删除别人的帖子");
            }
            TopicEntity topic = topicService.getById(post.getTopicId());
            String content = StrUtil.format(Constant.TOPIC_ADMIN_POST_DOWN, post.getTitle(), topic.getTopicName(), uid);
            messageService.sendMessageNotAsync(0, post.getUid(), -1, Constant.PUSHARTICLE, content, Constant.TITLE_VIOLATION);
        }
        this.removeById(id);
        deleteAllPostInfoById(id);
    }

    @Override
    public Integer getPostNumberByDiscussId(Integer id) {

        return Math.toIntExact(this.lambdaQuery()
                .eq(PostEntity::getDiscussId, id)
                .count());
    }

    @Override
    public Boolean setAdmin(SetAdminForm request, AppUserEntity user) {
        TopicAdminEntity topicAdmin = new TopicAdminEntity();
        BeanUtils.copyProperties(request, topicAdmin);
        topicAdmin.setCreateTime(DateUtil.nowDateTime());
        return topicAdminService.save(topicAdmin);
    }

    @Override
    public Boolean cancelAdmin(SetAdminForm request, AppUserEntity user) {
        TopicEntity topic = topicService.getById(request.getTopicId());
        if (topic.getUid().equals(request.getUid())) {
            throw new LinfengException("不能解除圈主权限");
        }
        LambdaQueryWrapper<TopicAdminEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TopicAdminEntity::getTopicId, request.getTopicId()).eq(TopicAdminEntity::getUid, request.getUid());
        return topicAdminService.remove(queryWrapper);
    }

    @Override
    @DSTransactional
    public void deleteByAdmin(List<Integer> ids) {
        //通知用户消息违规被删除了
        ids.forEach(postId -> {
            PostEntity post = this.getById(postId);
            String content = StrUtil.format(Constant.ADMIN_POST_DOWN, post.getTitle());
            messageService.sendMessageNotAsync(0, post.getUid(), -1, Constant.PUSHARTICLE, content, Constant.TITLE_VIOLATION);
            deleteAllPostInfoById(postId);
        });
        boolean remove = this.removeByIds(ids);
        if (!remove) {
            throw new LinfengException("批量删除失败");
        }
    }

    @Override
    public PostVipInfoResponse getVipPostInfo(VipPostInfoForm request) {
        PostEntity post = this.getById(request.getPostId());
        if (post == null) {
            throw new LinfengException("帖子不存在");
        }
        PostVipInfoResponse response = new PostVipInfoResponse();
        response.setTitle(post.getTitle());
        response.setBrief(post.getBrief());
        //如果是自己的帖子可以直接查看
        if (post.getUid().equals(request.getUid())) {
            response.setIsBuy(true);
        } else {
            boolean isPay = billService.vipPostIsPay(request.getPostId(), request.getUid());
            response.setIsBuy(isPay);
        }
        response.setPay(post.getPay());
        return response;
    }

    @Override
    @DSTransactional
    public Integer voteAdd(AddVoteForm request, AppUserEntity user) {

        VoteSubjectEntity voteSubject = new VoteSubjectEntity();
        voteSubject.setCreateTime(DateUtil.nowDateTime());
        voteSubject.setTitle(request.getVoteTitle());
        voteSubject.setType(request.getVoteType());
        if (request.getExpireTime() == 1) {
            voteSubject.setExpireTime(DateUtil.addDay(DateUtil.nowDateTimeStr(), 1));
        } else if (request.getExpireTime() == 2) {
            voteSubject.setExpireTime(DateUtil.addDay(DateUtil.nowDateTimeStr(), 7));
        } else if (request.getExpireTime() == 3) {
            voteSubject.setExpireTime(DateUtil.addDay(DateUtil.nowDateTimeStr(), 30));
        } else {
            voteSubject.setExpireTime(DateUtil.addDay(DateUtil.nowDateTimeStr(), 90));
        }
        if (voteSubjectService.save(voteSubject)) {
            request.getVoteOptions().forEach(e -> {
                VoteOptionEntity voteOption = new VoteOptionEntity();
                voteOption.setVoteId(voteSubject.getId());
                voteOption.setContent(e.getValue());
                voteOptionService.save(voteOption);
            });
            PostEntity post = new PostEntity();
            BeanUtils.copyProperties(request, post);
            post.setUid(user.getUid());
            post.setType(4);
            post.setCreateTime(DateUtil.nowDateTime());
            post.setVoteId(voteSubject.getId());
            if (this.save(post)) {
                return post.getId();
            }
        }
        return 0;
    }

    /**
     * 用户投票
     * @param request
     * @param user
     */
    @Override
    @DSTransactional
    public void userVote(UserVoteForm request, AppUserEntity user) {
        Long existingCount = voteResultService.lambdaQuery()
                .eq(VoteResultEntity::getVoteId, request.getId())
                .eq(VoteResultEntity::getUid, user.getUid())
                .count();
        if (ObjectUtil.isNotNull(existingCount) && existingCount > 0) {
            throw new LinfengException("您已投过票");
        }

        VoteResultEntity voteResult = new VoteResultEntity();
        voteResult.setCreateTime(DateUtil.nowDateTime());
        voteResult.setUid(user.getUid());
        voteResult.setVoteId(request.getId());
        voteResult.setResult(request.getVote().toString());
        boolean save = voteResultService.save(voteResult);
        if (!save) {
            throw new LinfengException("投票失败");
        }
        request.getVote().forEach(id -> {
            UpdateWrapper wrapper = new UpdateWrapper();
            wrapper.eq("id", id);
            wrapper.setSql("`ticket_num` = `ticket_num` + 1");
            boolean update = voteOptionService.update(wrapper);
            if (!update) {
                throw new LinfengException("投票失败");
            }
        });
    }


    @Override
    public void downByAdmin(DownPostForm param) {
        if (param.getIsSend() == 1 && WechatUtil.isEmpty(param.getDownReason())) {
            throw new LinfengException("请填写下架原因");
        }
        PostEntity post = this.getById(param.getId());
        post.setStatus(Constant.POST_BANNER);
        boolean update = this.updateById(post);
        if (!update) {
            throw new LinfengException("下架失败");
        }
        if (param.getIsSend() == 1) {
            String content = StrUtil.format(Constant.ADMIN_DOWN, post.getTitle(), param.getDownReason());
            messageService.sendMessage(0, post.getUid(), param.getId(), Constant.PUSHARTICLE, content, Constant.TITLE_VIOLATION);
        }
    }

    @Override
    public void upByAdmin(List<Integer> ids) {
        //通知用户帖子通过审核
        List<PostEntity> list = new ArrayList<>();
        ids.forEach(postId -> {
            PostEntity post = this.getById(postId);
            post.setStatus(Constant.POST_NORMAL);
            list.add(post);
            String content = StrUtil.format(Constant.ADMIN_POST_UP, post.getTitle());
            messageService.sendMessage(0, post.getUid(), postId, Constant.PUSHARTICLE, content, Constant.TITLE_PASS);
        });

        boolean b = this.updateBatchById(list);
        if (!b) {
            throw new LinfengException("审核失败");
        }
    }

    /**
     * 创建人机并自动发帖
     */
    @Override
    public void getRobotPostContent() {
        //创建人机
        List<String> list = new ArrayList<>();
        list.add("萌新");
        String mobile = "15" + RandomUtil.randomNumbers(9);
        Long num = appUserService.lambdaQuery().eq(AppUserEntity::getMobile, mobile).count();
        if (num > 0) {
            return;
        }
        AppUserEntity appUser = new AppUserEntity();
        appUser.setMobile(mobile);
//        appUser.setAvatar(Constant.DEAULT_HEAD);
        appUser.setGender(0);

        appUser.setUsername("love" + RandomUtil.randomNumbers(7));
        appUser.setTagStr(JSON.toJSONString(list));
        appUser.setCreateTime(DateUtil.nowDateTime());
        appUser.setUpdateTime(DateUtil.nowDateTime());
        appUser.setType(2);
        appUser.setAge(RandomUtil.randomInt(18, 59));

        Map<String, Map<String, List<SysArea>>> provincesMap = SysAreaService.provincesMap();
        // 步骤1：将Map的Key转为List（支持随机索引）
        List<String> keyList = new ArrayList<>(provincesMap.keySet());
        // 步骤2：生成随机索引（JDK 1.7+推荐用ThreadLocalRandom，性能更高）
        int randomIndex = ThreadLocalRandom.current().nextInt(keyList.size());
        int randomIndex2 = ThreadLocalRandom.current().nextInt(keyList.size());

        // 步骤3：获取随机Key和对应的Value
        String randomCity = keyList.get(randomIndex);
        String randomAbodeCity = keyList.get(randomIndex2);
        appUser.setHomeCity(randomCity);
        appUser.setAbodeCity(randomAbodeCity);
        appUser.setCity(randomCity);
        appUser.setEducation(RandomUtil.randomInt(0, 5));
        appUser.setHeight(RandomUtil.randomInt(150, 188)+"cm");
        appUser.setIncome(RandomUtil.randomInt(0, 5));
        SysUniversity sysUniversity = sysUniversityService.lambdaQuery().likeRight(SysUniversity::getUniName, randomCity).list().get(0);
        appUser.setSchool(sysUniversity.getUniName());
        JSONObject jsonObjectBaidu = restTemplateUtil.getData("https://chat.baidu.com/csaitab/searchresult?tk=d9ffa9ec?tk=MTIyNDA2ZmV8ZDQxZDhjZDk4ZjAwYjIwNGU5ODAwOTk4ZWNmODQyN2V8MTc2OTMzOTYyODEwM3w0Njk1MzI2MjIwNDU1NDUxNTE5-4695326220455451519-3&pn=0&rn=20&query=%E4%BD%9C%E4%B8%BA%E6%88%90%E5%8A%9F%E5%AD%A6%E5%A4%A7%E5%B8%88%EF%BC%8C%E8%AF%B7%E5%B8%AE%E6%88%91%E6%89%BE%E5%8D%81%E6%9D%A1%E5%BA%A7%E5%8F%B3%E9%93%AD&token=d9ffa9ec");
        JSONObject dataBaidu = jsonObjectBaidu.getJSONObject("data").getJSONObject("results");
        String self = dataBaidu.getString("abstract");
        appUser.setSelfIntroduction(self);
        String adm =dataBaidu.getJSONObject("data").getJSONObject("cambrian_us_showurl").getString("des");
                //NotionAI.write(NotionAI.spaceId, "写一首类似诗经风格的诗", "你是一个诗人");
        appUser.setAdminreHerart(adm);
        String inter ="啊啊啊啊";
        appUser.setInterest(inter);
        String love = "学会爱，所以被爱";
        appUser.setLoveDeclaration(love);
        appUserService.save(appUser);
        AppUserEntity appUsers = appUserService.lambdaQuery().eq(AppUserEntity::getMobile, mobile).one();
        //用户默认加入官方圈子
        topicService.joinTopic(2, appUsers);

        //随机发帖
        PostEntity post = new PostEntity();
        List<String> media = new ArrayList<>();

        post.setUid(appUsers.getUid());
        post.setCreateTime(DateUtil.nowDateTime());
        //请求开放接口获取短句和标题:todo:获取chatgpt 每日发送诗歌，散文等等
        //https://v1.hitokoto.cn/?c=e/:{"id":4306,"uuid":"bbe6102c-af5b-4e32-99a8-fd3777b24ce9","hitokoto":"我愿将歌化作风，聆听朗朗的春声。","type":"e","from":"村花上树","from_who":null,"creator":"村花上树梢","creator_uid":2824,"reviewer":0,"commit_from":"web","created_at":"1551444532","length":16}

        JSONObject data = restTemplateUtil.getData(Constant.ROBOT_CONTENT);
        if (ObjectUtil.isNull(data)) {
            return;
        }
        JSONObject result = data.getJSONObject("data");
        String content = result.getString("content");
        String origin = result.getString("origin");
        //请求开放接口获取图片
        int count = RandomUtil.randomInt(-2, 9);
        for (int i = 0; i < count; i++) {
            String link = restTemplateUtil.getLink(Constant.ROBOT_PIC);
            if (!link.equals("")) {
                JSONObject jsonObject = JSON.parseObject(link);
                String imgUrl = jsonObject.getString("imgurl");
                media.add(imgUrl);
            }
        }
        post.setMedia(JSON.toJSONString(media));
        post.setTitle(origin);
        post.setContent(content);
        post.setTopicId(2);
        post.setType(1);
        post.setReadCount(RandomUtil.randomInt(1, 20));
        this.save(post);
    }

    /**
     * 创建虚拟人机
     * @return 虚拟用户实体
     */
    @Override
    public AppUserEntity virtualUser() {
        List<String> list = new ArrayList<>();
        list.add(Constant.DEAULT_TAG);
        String mobile = "15" + RandomUtil.randomNumbers(9);
        Long num = appUserService.count(Wrappers.<AppUserEntity>lambdaQuery().eq(AppUserEntity::getMobile, mobile));
        if (num > 0) {
            return null;
        }
        AppUserEntity appUser = new AppUserEntity();
        appUser.setMobile(mobile);
        appUser.setAvatar(Constant.DEAULT_HEAD);
        appUser.setGender(GenderStatus.UNKNOWN.getValue());
        appUser.setUsername(WechatUtil.generateRandomName());
        appUser.setTagStr(JSON.toJSONString(list));
        appUser.setCreateTime(DateUtil.nowDateTime());
        appUser.setUpdateTime(DateUtil.nowDateTime());
        appUser.setType(UserTypeStatus.VIRTUALLY.getValue());
        appUserService.save(appUser);
        AppUserEntity appUsers = appUserService.getOne(Wrappers.<AppUserEntity>lambdaQuery().eq(AppUserEntity::getMobile, mobile));
        //用户默认加入官方圈子
        topicService.joinTopic(Constant.OFFICIAL_TOPIC_ID, appUsers);
        return appUsers;
    }

    /**
     * 设置帖子圈内置顶
     * @param request
     * @param user
     * @return
     */
    @Override
    public Boolean setPostTop(SetPostTopForm request, AppUserEntity user) {
        Boolean isAdmin = topicAdminService.isAdmin(user.getUid(), request.getTopicId());
        if (!isAdmin) {
            throw new LinfengException("非管理员无权限");
        }
        TopicTopEntity topicTop = topicTopService.lambdaQuery()
                .eq(TopicTopEntity::getTopicId, request.getTopicId())
                .eq(TopicTopEntity::getPostId, request.getPostId())
                .one();
        if (ObjectUtil.isNotNull(topicTop)) {
            throw new LinfengException("请勿重复置顶");
        }
        TopicTopEntity topicTopEntity = new TopicTopEntity();
        topicTopEntity.setPostId(request.getPostId());
        topicTopEntity.setTopicId(request.getTopicId());
        topicTopEntity.setCreateTime(DateUtil.nowDateTime());
        return topicTopService.save(topicTopEntity);
    }

    /**
     * 取消帖子圈内置顶
     * @param request
     * @param user
     * @return
     */
    @Override
    public Boolean topPostDel(SetPostTopForm request, AppUserEntity user) {
        Boolean isAdmin = topicAdminService.isAdmin(user.getUid(), request.getTopicId());
        if (!isAdmin) {
            throw new LinfengException("非管理员无权限");
        }
        TopicTopEntity topicTop = topicTopService.lambdaQuery()
                .eq(TopicTopEntity::getTopicId, request.getTopicId())
                .eq(TopicTopEntity::getPostId, request.getPostId())
                .one();
        if (ObjectUtil.isNull(topicTop)) {
            throw new LinfengException("不是置顶贴");
        }
        return topicTopService.removeById(topicTop.getId());
    }

    /**
     * 生成帖子详情海报图
     * @param postId  帖子id
     * @param origin 来源
     * @param url  分享全路径
     * @param user 当前用户
     * @return 海报路径
     * @throws Exception
     */
    @Override
    public String getSharePic(Integer postId, String origin, String url, AppUserEntity user) throws Exception {
        PostEntity post = this.getById(postId);
        if (null == post) {
            throw new LinfengException("帖子不存在或已删除");
        }
        String showPic;
        if (null == post.getMedia()) {
            showPic = Constant.DEAULT_SHARE_POST;
        } else {
            List<String> list = JsonUtils.JsonToList(post.getMedia());
            if (list.isEmpty()) {
                showPic = Constant.DEAULT_SHARE_POST;
            } else {
                showPic = list.get(0);
            }
        }
        String itemName = post.getTitle();

        int width = 640;
        int height = 1052;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = image.createGraphics();

        image = g2d.getDeviceConfiguration().createCompatibleImage(width, height, Transparency.OPAQUE);
        g2d.dispose();
        g2d = image.createGraphics();

        g2d.setBackground(Color.WHITE);
        g2d.clearRect(0, 0, width, height);

        g2d.setColor(new Color(182, 249, 225));
        g2d.setStroke(new BasicStroke(1));
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        BufferedImage proImg = WechatUtil.readImgInner(showPic);
        if (proImg == null) {
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

        BufferedImage shareHeadImg = WechatUtil.readImgInner(user.getAvatar());
        if (shareHeadImg == null) {
            throw new LinfengException("头像图片不是https格式");
        }
        int baseHeight = 610;


        g2d.setPaint(Color.GRAY);
        File newFileT = new File("simsunb.ttf");
        if (!newFileT.exists()) {
            InputStream streamT = getClass().getClassLoader()
                    .getResourceAsStream("simsunb.ttf");
            FileUtils.copyInputStreamToFile(streamT, newFileT);
        }
        Font font = Font.createFont(Font.TRUETYPE_FONT, newFileT);
        g2d.setFont(font.deriveFont(Font.PLAIN, 27));
        g2d.drawImage(shareHeadImg.getScaledInstance(120, 120, Image.SCALE_SMOOTH), 50, baseHeight, null);
        g2d.drawString(user.getUsername() + "分享了一条动态", 210, baseHeight + 60);
        //简介
        g2d.setPaint(Color.BLACK);

        final Font logoFont = font.deriveFont(Font.PLAIN, 21);
        g2d.setFont(logoFont);
        FontMetrics fm2 = g2d.getFontMetrics(g2d.getFont());
        int textWidth2 = fm2.stringWidth(itemName);
        while (textWidth2 > 260 && itemName.length() > 2) {
            itemName = itemName.substring(0, itemName.length() - 1);
            textWidth2 = fm2.stringWidth(itemName);
        }
        g2d.setPaint(Color.GRAY);
        final Font logos = font.deriveFont(Font.BOLD, 30);
        g2d.setFont(logos);
        g2d.drawString(itemName, 50, baseHeight + 190);
        Font fonts = Font.createFont(Font.TRUETYPE_FONT, newFileT);
        g2d.setPaint(Color.BLACK);
        g2d.setFont(fonts.deriveFont(Font.BOLD, 17));
        BufferedImage qrCode = null;
        if (origin.equals(Constant.H5)) {
            g2d.drawString("扫描二维码查看动态详情", 30, baseHeight + 336);
            qrCode = QRCodeUtils.createImage(url, null, true);
        } else if (origin.equals(Constant.WX)) {
            g2d.drawString("微信扫码查看动态详情", 30, baseHeight + 336);
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
        String urls= OssFactory.instance().uploadSuffix(inputStream, "post").getUrl();

        return OSSFactory.build().uploadSuffix(inputStream, "post");
    }

    @Override
    public List<PostEntity> getTopPost() {
        return this.lambdaQuery()
                .eq(PostEntity::getPostTop, 1)
                .eq(PostEntity::getType, Constant.POST_TYPE_TEXT)
                .orderByDesc(PostEntity::getReadCount)
                .last("limit 1")
                .list();
    }

    @Override
    public List<PostEntity> getHotPost() {
//        DateTime dateTime = cn.hutool.core.date.DateUtil.lastMonth();//近一个月最热帖子
        DateTime dateTime = cn.hutool.core.date.DateUtil.lastWeek();//近一周最热帖子
        return this.lambdaQuery()
                .eq(PostEntity::getPostTop, 0)
                .eq(PostEntity::getStatus, Constant.POST_NORMAL)
                .eq(PostEntity::getType, Constant.POST_TYPE_TEXT)
                .eq(PostEntity::getIsPrivate, PrivacyStatus.PUBLIC.getValue())
                .gt(PostEntity::getCreateTime, dateTime)
                .orderByDesc(PostEntity::getReadCount)
                .last("limit 10")
                .list();
    }

    @Override
    public AppPageUtils getPostListByType(Integer page, Integer type) {
        QueryWrapper<PostEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PostEntity::getStatus, Constant.POST_NORMAL);
        queryWrapper.lambda().eq(PostEntity::getIsPrivate, PrivacyStatus.PUBLIC.getValue());
        queryWrapper.lambda().eq(PostEntity::getType, type);
        queryWrapper.lambda().orderByDesc(PostEntity::getId);
        Page<PostEntity> pages = new Page<>(page, 10);
        return this.mapPostList(pages, queryWrapper, 0);
    }

    @Override
    public Integer addArticle(AddArticleForm request, Integer uid) {
        AppUserEntity user = appUserService.getById(uid);
        if (user.getStatus() != 0) {
            throw new LinfengException("您的账号已被禁用");
        }
        sensitiveService.checkPostContent(request.getTitle());
        PostEntity post = new PostEntity();
        BeanUtils.copyProperties(request, post);
        post.setUid(user.getUid());
        post.setCreateTime(DateUtil.nowDateTime());
        String media = JSON.toJSONString(request.getMedia());
        post.setMedia(media);
        String normalPost = configService.getValue(Constant.NORMAL_POST);
        if (normalPost.equals("0")) {
            post.setStatus(Constant.POST_REVIEWED);
        }
        TopicEntity topic = topicService.getById(request.getTopicId());
        post.setIsPrivate(topic.getIsPrivacy());
        if (this.save(post)) {
            //发帖奖励积分
            rewardIntegralByPosting(user);
            return post.getId();
        }
        return 0;
    }

    @Override
    public void postAddByAdmin(AddPostByAdminForm request) {
        PostEntity post = new PostEntity();
        BeanUtils.copyProperties(request, post);
        String media = JSON.toJSONString(request.getMedia());
        post.setMedia(media);
        post.setCreateTime(DateUtil.nowDateTime());
        boolean save = this.save(post);
        if (!save) {
            throw new LinfengException("发帖失败");
        }
    }

    @Override
    public List<String> getImagePostListByUser(Integer uid) {
        return postDao.getImagePostListByUser(uid);
    }

    @Override
    public AppPageUtils queryShortVideoPageList(VideoListForm request) {
        Page<PostEntity> page = new Page<>(request.getPage(), 10);
        QueryWrapper<PostEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PostEntity::getStatus, Constant.POST_NORMAL);
        queryWrapper.lambda().eq(PostEntity::getType, Constant.POST_TYPE_VIDEO);
        queryWrapper.lambda().eq(PostEntity::getIsPrivate, PrivacyStatus.PUBLIC.getValue());
        queryWrapper.lambda().eq(PostEntity::getCut, 0);
        if (request.getActivityId() != null && request.getActivityId() > 0) {
            queryWrapper.lambda().eq(PostEntity::getActivityId, request.getActivityId());
        }
        if (request.getOrder() != null && request.getOrder() == 1) {
            queryWrapper.lambda().orderByDesc(PostEntity::getId);
        } else {
            //热门浏览视频  限制时间查询 查询近180天的热门视频 可自定
            String dateTime = DateUtils.addDateDays(new Date(), -180);
            queryWrapper.lambda().gt(PostEntity::getCreateTime, dateTime);
            queryWrapper.lambda().orderByDesc(PostEntity::getReadCount);
        }

        // 尝试从 Sa-Token 获取当前登录用户
        Integer uid = 0;
        try {
            if (StpUtil.isLogin()) {
                Object loginId = StpUtil.getLoginIdDefaultNull();
                if (loginId != null) {
                    uid = Integer.parseInt(loginId.toString());
                    log.debug("[视频列表] Sa-Token获取到用户uid={}", uid);
                }
            }
        } catch (Exception e) {
            log.debug("[视频列表] Sa-Token获取用户失败: {}", e.getMessage());
        }
        
        AppPageUtils appPage = this.mapPostList(page, queryWrapper, uid);
        List<PostListResponse> data = (List<PostListResponse>) appPage.getData();
        List<ShortVideoListResponse> shortVideoList = new ArrayList<>();
        data.forEach(item -> {
            ShortVideoListResponse response = new ShortVideoListResponse();
            BeanUtils.copyProperties(item, response);
            response.setMediasList(new ArrayList<>());
            response.set_id(item.getId() + "");
            response.setIsplay(true);
            response.setPlayIng(false);
            //视频播放源加密
            response.setSrc(UrlEncryptor.encryptUrl(item.getMedia().get(0)));
            response.setState("pause");
            shortVideoList.add(response);
        });
        appPage.setData(shortVideoList);
        return appPage;
    }

    @Override
    public void addReadCount(Integer postId) {
        PostEntity post = this.getById(postId);
        if (post == null) {
            return;
        }
        post.setReadCount(post.getReadCount() + 1);
        baseMapper.updateById(post);
    }

    @Override
    @DSTransactional
    public void deletePostIdByAdmin(DeletePostForm param) {
        PostEntity post = this.getById(param.getId());
        if (param.getIsSendDeleteInfo() == 1) {
            String content = StrUtil.format(Constant.ADMIN_POST_DOWN_REASON, post.getTitle(), param.getDeleteReason());
            messageService.sendMessageNotAsync(0, post.getUid(), -1, Constant.PUSHARTICLE, content, Constant.TITLE_VIOLATION);
        }
        this.removeById(param.getId());
        deleteAllPostInfoById(param.getId());
    }

    /**
     * 发帖奖励积分
     * @param userInfo
     */
    private void rewardIntegralByPosting(AppUserEntity userInfo) {
        AppUserEntity user = userInfo;
        if (user.getVip().equals(Constant.VIP_USER)) {
            boolean isBefore = user.getVipExpireTime().before(DateUtil.nowDateTime());
            if (isBefore) {
                user.setVip(Constant.COMMON_USER);
                this.appUserService.updateById(user);
            }
        }
        String addPostIntegral = configService.getValue(Constant.ADD_POST_INTEGRAL);
        String addPostIntegralLimit = configService.getValue(Constant.ADD_POST_INTEGRAL_LIMIT);
        Integer integral = Integer.valueOf(addPostIntegral);
        Integer limitCount = Integer.valueOf(addPostIntegralLimit);
        if (integral <= 0 || limitCount <= 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String startTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00:00"));
        String endTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd 23:59:59"));
        Timestamp startTimestamp = Timestamp.valueOf(startTime);
        Timestamp endTimestamp = Timestamp.valueOf(endTime);
        Integer count = Math.toIntExact(this.lambdaQuery()
                .eq(PostEntity::getUid, userInfo.getUid())
                .between(PostEntity::getCreateTime, startTimestamp, endTimestamp)
                .count());
        if (count > limitCount) {
            return;
        }
        Integer gain = integral;
        if (user.getVip().equals(Constant.VIP_USER)) {
            String value = configService.getValue(Constant.VIP_INTEGRAL);
            Integer multiple = Integer.valueOf(value);
            if (multiple > 0) {
                gain = integral * multiple;
            }
        }
        boolean update = appUserService.lambdaUpdate()
                .set(AppUserEntity::getIntegral, user.getIntegral() + gain)
                .eq(AppUserEntity::getUid, user.getUid())
                .update();
        if (!update) {
            throw new LinfengException("用户积分更新失败");
        }
        org.aileme.common.redis.utils.RedisUtils.deleteObject(RedisKeys.getUserCacheKey(user.getUid()));
        //积分账单插入
        billService.income(user.getUid(), "发帖积分奖励", BillDetailEnum.CATEGORY_2.getValue(),
                BillDetailEnum.TYPE_9.getValue(), gain, user.getIntegral().doubleValue(),
                "发帖积分奖励", "", null);
        userLevelService.checkUserLevel(user.getUid());
    }

    /**
     * 帖子删除后清理其他关联表数据
     * @param postId
     */
    private void deleteAllPostInfoById(Integer postId) {
        //1.清理点赞表相关数据
        LambdaQueryWrapper<PostCollectionEntity> postCollectionWrapper = new LambdaQueryWrapper<>();
        postCollectionWrapper.eq(PostCollectionEntity::getPostId, postId);
        postCollectionService.remove(postCollectionWrapper);
        //2.清理评论点赞表相关数据
        List<CommentEntity> list = commentService.lambdaQuery().eq(CommentEntity::getPostId, postId).list();
        if (!list.isEmpty()) {
            List<Integer> idList = list.stream().map(CommentEntity::getId).collect(Collectors.toList());
            List<CommentThumbsEntity> commentThumbsList = commentThumbsService.lambdaQuery().in(CommentThumbsEntity::getCId, idList).list();
            if (!commentThumbsList.isEmpty()) {
                List<Integer> commentThumbsIdList = commentThumbsList.stream().map(CommentThumbsEntity::getId).collect(Collectors.toList());
                commentThumbsService.removeByIds(commentThumbsIdList);
            }
        }
        //3.清理评论表相关数据
        LambdaQueryWrapper<CommentEntity> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.eq(CommentEntity::getPostId, postId);
        commentService.remove(commentWrapper);
        //4.删除消息中心关于该帖子的消息
        LambdaQueryWrapper<MessageEntity> messageWrapper = new LambdaQueryWrapper<>();
        messageWrapper.eq(MessageEntity::getPostId, postId);
        messageService.remove(messageWrapper);
        //5.清理圈内置顶贴表数据
        LambdaQueryWrapper<TopicTopEntity> topicTopWrapper = new LambdaQueryWrapper<>();
        topicTopWrapper.eq(TopicTopEntity::getPostId, postId);
        topicTopService.remove(topicTopWrapper);
        //6.清理redis数据
        org.aileme.common.redis.utils.RedisUtils.deleteObject(RedisKeys.getPostKey(postId));
    }

    /**
     * 组装帖子分页
     * @param page 分页
     * @param queryWrapper  查询条件
     * @param uid  用户id
     * @return 用户端帖子分页列表
     */
    public AppPageUtils mapPostList(Page<PostEntity> page, QueryWrapper<PostEntity> queryWrapper, Integer uid) {
        Page<PostEntity> pages = baseMapper.selectPage(page, queryWrapper);
        if (pages.getRecords().isEmpty()) {
            return new AppPageUtils(pages);
        }
        AppPageUtils appPage = new AppPageUtils(pages);
        List<PostEntity> data = (List<PostEntity>) appPage.getData();
        List<PostListResponse> responseList = new ArrayList<>();
        List<Integer> uidList = data.stream().map(PostEntity::getUid).collect(Collectors.toList());
        List<Integer> postIdList = data.stream().map(PostEntity::getId).collect(Collectors.toList());
        List<Integer> topicIdList = data.stream().map(PostEntity::getTopicId).collect(Collectors.toList());
        List<Integer> activityIdList = data.stream()
                .map(PostEntity::getActivityId)
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .collect(Collectors.toList());
        List<Integer> voteIdList = data.stream()
                .map(PostEntity::getVoteId)
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .collect(Collectors.toList());
        List<Integer> videoIdList = data.stream()
                .map(PostEntity::getVideoId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<CommentCountResponse> commentList = commentService.getAllCountByPostId(postIdList);
        Map<Integer, String> topicNameMap = topicService.getAllByList(topicIdList);
        Map<Integer, XiangqinActivityEntity> activityMap = activityIdList.isEmpty()
                ? new HashMap<>()
                : xiangqinActivityService.listByIds(activityIdList).stream()
                .collect(Collectors.toMap(XiangqinActivityEntity::getId, Function.identity()));
        Map<Integer, Integer> commentMap = commentList.stream().collect(Collectors.toMap(CommentCountResponse::getPostId, CommentCountResponse::getNumber));
        List<AppUserEntity> appUserList = appUserService.listByIds(uidList);
        Map<Integer, AppUserEntity> userMap = appUserList.stream().collect(Collectors.toMap(AppUserEntity::getUid, Function.identity()));
        List<PostCountResponse> postCollectionCount = postCollectionService.findBatchCollectCount(postIdList);
        Map<Integer, Integer> postCollectionMap = postCollectionCount.stream().collect(Collectors.toMap(PostCountResponse::getPostId, PostCountResponse::getNumber));

        Map<Integer, Integer> followCollectMap = new HashMap<>();
        Map<Integer, Integer> isCollectBatchMap = new HashMap<>();
        if (uid != 0) {
            List<FollowBatchResponse> followBatch = followService.findFollowBatch(uidList, uid);
            followCollectMap = followBatch.stream().collect(Collectors.toMap(FollowBatchResponse::getFollowUid, FollowBatchResponse::getId));
            List<PostIsCollectionResponse> isCollectBatchList = postCollectionService.findIsCollectBatch(postIdList, uid);
            isCollectBatchMap = isCollectBatchList.stream().collect(Collectors.toMap(PostIsCollectionResponse::getPostId, PostIsCollectionResponse::getUid));
        }
        Set<Integer> votedVoteIdSet = new HashSet<>();
        if (uid != 0 && !voteIdList.isEmpty()) {
            votedVoteIdSet = voteResultService.lambdaQuery()
                    .select(VoteResultEntity::getVoteId)
                    .eq(VoteResultEntity::getUid, uid)
                    .in(VoteResultEntity::getVoteId, voteIdList)
                    .list()
                    .stream()
                    .map(VoteResultEntity::getVoteId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }
        Map<Integer, Integer> finalFollowCollectMap = followCollectMap;
        Map<Integer, Integer> finalIsCollectBatchMap = isCollectBatchMap;
        Set<Integer> finalVotedVoteIdSet = votedVoteIdSet;
        Map<Integer, String> videoCoverMap = new HashMap<>();
        Map<Integer, String> videoUrlMap = new HashMap<>();
        if (!videoIdList.isEmpty()) {
            List<UserVideoEntity> playableVideos = userVideoService.getPlayableVideos(videoIdList);
            videoCoverMap = playableVideos.stream().collect(Collectors.toMap(
                    UserVideoEntity::getId,
                    item -> JsonUtils.normalizeUrlValue(item.getCoverUrl())
            ));
            videoUrlMap = playableVideos.stream().collect(Collectors.toMap(
                    UserVideoEntity::getId,
                    item -> JsonUtils.normalizeUrlValue(item.getVideoUrl())
            ));
        }
        Map<Integer, String> finalVideoCoverMap = videoCoverMap;
        Map<Integer, String> finalVideoUrlMap = videoUrlMap;

        data.forEach(post -> {
            PostListResponse response = new PostListResponse();
            BeanUtils.copyProperties(post, response);
            if (ObjectUtil.isNotNull(response.getDiscussId()) && response.getDiscussId() > 0) {
                DiscussEntity discussEntity = discussService.getById(response.getDiscussId());
                if (ObjectUtil.isNotNull(discussEntity)) {
                    response.setDiscussTitle(discussEntity.getTitle());
                }
            }
            if (post.getActivityId() != null && post.getActivityId() > 0) {
                XiangqinActivityEntity activity = activityMap.get(post.getActivityId());
                if (activity != null) {
                    response.setActivityId(activity.getId());
                    response.setActivityTitle(activity.getTitle());
                    response.setActivityStatus(activity.getStatus());
                }
            }
            response.setTopicName(topicNameMap.get(post.getTopicId()));
            if (ObjectUtil.isNotNull(postCollectionMap.get(post.getId()))) {
                response.setCollectionCount(postCollectionMap.get(post.getId()));
                response.setLikeCount(postCollectionMap.get(post.getId()));
            } else {
                response.setCollectionCount(0);
                response.setLikeCount(0);
            }
            if (ObjectUtil.isNotNull(commentMap.get(post.getId()))) {
                response.setCommentCount(commentMap.get(post.getId()));
            } else {
                response.setCommentCount(0);
            }
            if (ObjectUtil.isNotNull(userMap.get(post.getUid()))) {
                AppUserShortInfoResponse shortInfoResponse = new AppUserShortInfoResponse();
                BeanUtils.copyProperties(userMap.get(post.getUid()), shortInfoResponse);
                response.setUserInfo(shortInfoResponse);
            }
            if (uid != 0) {
                if (ObjectUtil.isNotNull(finalFollowCollectMap.get(post.getUid()))) {
                    response.setIsFollow(true);
                } else {
                    response.setIsFollow(false);
                }
                if (ObjectUtil.isNotNull(finalIsCollectBatchMap.get(post.getId()))) {
                    response.setIsCollection(true);
                    response.setIsLike(true);
                } else {
                    response.setIsCollection(false);
                    response.setIsLike(false);
                }
            } else {
                response.setIsFollow(false);
                response.setIsCollection(false);
                response.setIsLike(false);
            }
            if (post.getVideoId() != null) {
                response.setCoverUrl(finalVideoCoverMap.get(post.getVideoId()));
            }
            //付费贴去除内容
            if (Objects.equals(post.getCut(), 1)) {
                List<String> list = new ArrayList<>();
                list.add(Constant.DEAULT_VIP_POST);
                response.setMedia(list);
                response.setContent(response.getTitle());//这里要去掉具体内容替换为标题，标题可见
            } else {
                response.setMedia(JsonUtils.JsonToList(post.getMedia()));
            }
            if (post.getVideoId() != null && StrUtil.isNotBlank(finalVideoUrlMap.get(post.getVideoId()))) {
                response.setMedia(Collections.singletonList(finalVideoUrlMap.get(post.getVideoId())));
            }
            if (post.getVoteId() != null && post.getVoteId() > 0) {
                VoteSubjectEntity voteSubject = voteSubjectService.lambdaQuery().eq(VoteSubjectEntity::getId, post.getVoteId()).one();
                if (voteSubject != null) {
                    List<VoteOptionEntity> voteOptionList = voteOptionService.lambdaQuery().eq(VoteOptionEntity::getVoteId, post.getVoteId()).list();
                    VoteInfoResponse voteInfoResponse = new VoteInfoResponse();
                    BeanUtils.copyProperties(voteSubject, voteInfoResponse);
                    voteInfoResponse.setOptions(voteOptionList);
                    voteInfoResponse.setTime(voteSubject.getExpireTime());
                    response.setVoteInfo(voteInfoResponse);
                } else {
                    log.warn("帖子投票主数据缺失, postId={}, voteId={}", post.getId(), post.getVoteId());
                }
                response.setIsVoteResult(finalVotedVoteIdSet.contains(post.getVoteId()));
            } else {
                response.setIsVoteResult(false);
            }
            responseList.add(response);
        });
        appPage.setData(responseList);
        return appPage;
    }

    private void validateActivityLinkPermission(Integer activityId, AppUserEntity user) {
        if (activityId == null || activityId <= 0) {
            return;
        }
        XiangqinActivityEntity activity = xiangqinActivityService.getById(activityId);
        if (activity == null) {
            throw new LinfengException("关联的活动不存在");
        }
        Integer currentHongniangId = user.getHongniangId();
        if ((currentHongniangId == null || currentHongniangId <= 0) && user.getUid() != null) {
            HongniangInfoEntity hongniang = hongniangService.getByUserId(user.getUid());
            if (hongniang != null) {
                currentHongniangId = hongniang.getId();
            }
        }
        boolean isActivityOwner = currentHongniangId != null
                && activity.getHongniangId() != null
                && Objects.equals(currentHongniangId, activity.getHongniangId());
        if (!isActivityOwner && !xiangqinEnrollmentService.checkEnrolled(activityId, user.getUid())) {
            throw new LinfengException("仅活动参与者或主理人可关联该活动发布内容");
        }
    }


}
