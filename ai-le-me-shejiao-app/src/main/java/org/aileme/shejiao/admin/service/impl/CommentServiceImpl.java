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
import com.baomidou.dynamic.datasource.annotation.DS;

import java.time.Duration;

import cn.hutool.core.util.NumberUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.aileme.shejiao.domain.enums.CommentStatus;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.domain.vo.AppChildrenCommentResponse;
import org.aileme.shejiao.domain.vo.AppCommentResponse;
import org.aileme.shejiao.domain.vo.AppUserShortInfoResponse;
import org.aileme.shejiao.domain.vo.CommentCountResponse;
import org.aileme.shejiao.common.utils.*;
import org.aileme.shejiao.admin.utils.WechatUtil;import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.domain.param.app.DelCommentForm;
import org.aileme.shejiao.api.service.CommentThumbsService;
import org.aileme.shejiao.admin.utils.LocalUser;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.aileme.shejiao.admin.dao.CommentDao;
import org.aileme.shejiao.domain.entity.admin.CommentEntity;
import org.aileme.shejiao.api.service.CommentService;
import org.redisson.client.codec.StringCodec;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;


@DS("master")
@Service("commentService")
public class CommentServiceImpl extends ServiceImpl<CommentDao, CommentEntity> implements CommentService {

    @Autowired
    private AppUserService appUserService;
    @Autowired
    private CommentThumbsService commentThumbsService;
    @Autowired
    private LocalUser localUser;
    @Autowired
    private CommentDao commentDao;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {

        QueryWrapper<CommentEntity> queryWrapper=new QueryWrapper<>();
        //模糊查询
        String key = (String)params.get("key");
        String status = (String)params.get("status");
        if(!WechatUtil.isEmpty(key)){
            if(NumberUtil.isInteger(key)){
                queryWrapper.lambda().eq(CommentEntity::getPostId, key);
            }else{
                queryWrapper.lambda().like(CommentEntity::getContent, key);
            }
        }
        if(!WechatUtil.isEmpty(status)){
            queryWrapper.lambda().eq(CommentEntity::getStatus, Integer.parseInt(status));
        }

        queryWrapper.lambda().orderByDesc(CommentEntity::getId);
        IPage<CommentEntity> page = this.page(
                new Query<CommentEntity>().getPage(params),
                queryWrapper
        );
        return new PageUtils(page);
    }

    @Override
    public Integer getCountByPostId(Integer id) {
        Object hashValue = org.aileme.common.redis.utils.RedisUtils.getClient().getMap(RedisKeys.getPostKey(id), StringCodec.INSTANCE).get(ConfigConstant.POST_COMMENT_NUM);
        if(hashValue==null){
            Integer num = Math.toIntExact(baseMapper.selectCount(
                    new LambdaQueryWrapper<CommentEntity>()
                            .eq(CommentEntity::getStatus, CommentStatus.NORMAL.getValue())
                            .eq(CommentEntity::getPostId, id)));
            org.aileme.common.redis.utils.RedisUtils.getClient().getMap(RedisKeys.getPostKey(id), StringCodec.INSTANCE).put(ConfigConstant.POST_COMMENT_NUM, num.toString());
            return num;
        }else{
            return Integer.valueOf(hashValue.toString());
        }
    }

    @Override
    public List<CommentCountResponse> getAllCountByPostId(List<Integer> list) {
        List<Integer> resultList=new ArrayList<>();
        List<CommentCountResponse> cacheCollectList =new ArrayList<>();
        list.forEach(postId->{
            Object hashValue = org.aileme.common.redis.utils.RedisUtils.getClient().getMap(RedisKeys.getPostKey(postId), StringCodec.INSTANCE).get(ConfigConstant.POST_COMMENT_NUM);
            if(hashValue==null){
                resultList.add(postId);
            }else{
                CommentCountResponse response=new CommentCountResponse();
                response.setNumber(Integer.valueOf(hashValue.toString()));
                response.setPostId(postId);
                cacheCollectList.add(response);
            }
        });
        //如果redis缓存中没有 去数据库批量查询
        if(!resultList.isEmpty()){
            List<CommentCountResponse> batchCollectCount = commentDao.getAllCountByPostId(resultList);
            //数据库查询后也存入redis缓存
            batchCollectCount.forEach(item->{
                org.aileme.common.redis.utils.RedisUtils.getClient().getMap(RedisKeys.getPostKey(item.getPostId()), StringCodec.INSTANCE).put(ConfigConstant.POST_COMMENT_NUM, item.getNumber().toString());
            });
            cacheCollectList.addAll(batchCollectCount);
        }
        return cacheCollectList;
    }

    @Override
    public AppPageUtils queryCommentPage(Integer postId, Integer page) {
        Page<CommentEntity> commentPage = new Page<>(page,10);
        QueryWrapper<CommentEntity> queryWrapper=new QueryWrapper<>();
        queryWrapper.lambda().eq(CommentEntity::getPostId,postId);
        queryWrapper.lambda().eq(CommentEntity::getStatus,CommentStatus.NORMAL.getValue());
        queryWrapper.lambda().orderByDesc(CommentEntity::getId);
        Page<CommentEntity> pages = baseMapper.selectPage(commentPage,queryWrapper);
        AppPageUtils appPage=new AppPageUtils(pages);
        List<CommentEntity> data = (List<CommentEntity>) appPage.getData();
        List<AppCommentResponse> responseList=new ArrayList<>();
        AppUserEntity user = localUser.getUser();
        data.forEach(l -> {
            if (l.getPid() == 0) {  //过滤子评论
                AppCommentResponse response = new AppCommentResponse();
                BeanUtils.copyProperties(l, response);
                AppUserEntity appUserEntity = appUserService.getById(response.getUid());
                AppUserShortInfoResponse userInfoVo=new AppUserShortInfoResponse();
                BeanUtils.copyProperties(appUserEntity,userInfoVo);
                response.setUserInfo(userInfoVo);
                response.setThumbs(commentThumbsService.getThumbsCount(l.getId()));
                response.setShowBtn(true);
                if(user==null){
                    response.setIsThumbs(false);
                }else{
                    response.setIsThumbs(commentThumbsService.isThumbs(user.getUid(), l.getId()));
                }

                if (l.getMedia() != null) {
                    response.setMedias(JsonUtils.JsonToList(l.getMedia()));
                }
                //处理子评论
                List<CommentEntity> childrenCommentList = this.getByPid(l.getId());
                response.setChildrenCount(childrenCommentList.size());
                if (childrenCommentList.isEmpty()) {
                    response.setChildren(new ArrayList<>());
                } else {
                    List<AppChildrenCommentResponse> childrenList = new ArrayList<>();
                    //最多只读取2条子评论 剩下的用户请求了再单独读取
                    Integer commentNum = Math.min(2, childrenCommentList.size());
                    for (int i = 0; i < commentNum ; i++) {
                        CommentEntity childrenComment = childrenCommentList.get(i);
                        AppChildrenCommentResponse childrenResponse = new AppChildrenCommentResponse();
                        BeanUtils.copyProperties(childrenComment, childrenResponse);
                        AppUserEntity commentUser = appUserService.getById(childrenComment.getUid());
                        AppUserShortInfoResponse userVo=new AppUserShortInfoResponse();
                        BeanUtils.copyProperties(commentUser,userVo);
                        childrenResponse.setUserInfo(userVo);
                        AppUserEntity toUserInfo = appUserService.getById(childrenComment.getToUid());
                        AppUserShortInfoResponse toUserInfoVo=new AppUserShortInfoResponse();
                        BeanUtils.copyProperties(toUserInfo,toUserInfoVo);
                        childrenResponse.setToUser(toUserInfoVo);
                        childrenResponse.setThumbs(commentThumbsService.getThumbsCount(childrenComment.getId()));
                        if(user==null){
                            childrenResponse.setIsThumbs(false);
                        }else{
                            childrenResponse.setIsThumbs(commentThumbsService.isThumbs(user.getUid(), childrenComment.getId()));
                        }
                        if (childrenComment.getMedia() != null) {
                            childrenResponse.setMedias(JsonUtils.JsonToList(childrenComment.getMedia()));
                        }
                        childrenList.add(childrenResponse);
                    }
                    response.setChildren(childrenList);
                }
                responseList.add(response);
            }
        });
        appPage.setData(responseList);
        return appPage;
    }

    @Override
    public List<CommentEntity> getByPid(Integer pid) {
        return baseMapper.selectList(
                new LambdaQueryWrapper<CommentEntity>()
                        .eq(CommentEntity::getStatus,CommentStatus.NORMAL.getValue())
                        .eq(CommentEntity::getPid, pid));
    }

    /**
     * 用户端-长按评论-删除自己评论
     * @param request
     * @param user
     */
    @Override
    @DSTransactional
    public void del(DelCommentForm request, AppUserEntity user) {
        CommentEntity comment = this.getById(request.getId());
        if(comment == null){
            throw new LinfengException("请先长按评论再删除");
        }
        if (comment.getUid().intValue() != user.getUid()) {
            throw new LinfengException("不能删除别人的评论");
        }
        //评论数量缓存修改
        List<CommentEntity> list = this.lambdaQuery().eq(CommentEntity::getPid, comment.getId()).list();
        org.aileme.common.redis.utils.RedisUtils.getClient().getMap(RedisKeys.getPostKey(comment.getPostId()), StringCodec.INSTANCE).addAndGet(ConfigConstant.POST_COMMENT_NUM, -(1+list.size()));
        //子评论更改展示状态为屏蔽
        this.lambdaUpdate()
                .set(CommentEntity::getStatus, CommentStatus.OFF.getValue())
                .eq(CommentEntity::getPid, comment.getId())
                .update();
        this.removeById(request.getId());

        //清理评论列表缓存
        org.aileme.common.redis.utils.RedisUtils.deleteObject(ConfigConstant.COMMENT_KEY+comment.getPostId());
    }

    /**
     * 管理端批量删除评论
     * @param list
     */
    @Override
    @DSTransactional
    public void deleteByAdmin(List<Long> list) {
        list.forEach(id->{
            CommentEntity comment = this.getById(id);
            //清理评论列表缓存
            List<CommentEntity> lists = this.lambdaQuery().eq(CommentEntity::getPid, id).list();
            org.aileme.common.redis.utils.RedisUtils.getClient().getMap(RedisKeys.getPostKey(comment.getPostId()), StringCodec.INSTANCE).addAndGet(ConfigConstant.POST_COMMENT_NUM, -(lists.size()+1));
            org.aileme.common.redis.utils.RedisUtils.deleteObject(ConfigConstant.COMMENT_KEY+comment.getPostId());
            this.removeById(id);
            //子评论更改展示状态为屏蔽
            this.lambdaUpdate()
                    .set(CommentEntity::getStatus, CommentStatus.OFF.getValue())
                    .eq(CommentEntity::getPid, id)
                    .update();
        });
    }


    /**
     * 从缓存获取评论列表第一页数据
     *
     * @return
     */
    @Override
    public JSONObject getCommentListInCache(Integer postId){
        String result = org.aileme.common.redis.utils.RedisUtils.getCacheObject(ConfigConstant.COMMENT_KEY+postId);
        if(!WechatUtil.isEmpty(result)){
            return JSONObject.parseObject(result);
        }
        AppPageUtils appPage = this.queryCommentPage(postId, 1);
        // 统一使用 JSON 字符串存储到 Redis
        String jsonStr = JSONObject.toJSONString(appPage, SerializerFeature.WriteDateUseDateFormat);
        org.aileme.common.redis.utils.RedisUtils.setCacheObject(ConfigConstant.COMMENT_KEY+postId, jsonStr, Duration.ofSeconds(60*60));
        return JSONObject.parseObject(jsonStr);
    }

    @Override
    public void updateCommentById(CommentEntity comment) {
        CommentEntity commentEntity = this.getById(comment.getId());
        if(!comment.getStatus().equals(commentEntity.getStatus())){
            List<CommentEntity> lists = this.lambdaQuery().eq(CommentEntity::getPid, comment.getId()).list();
            //如果是修改待审核评论，修改为上架才做缓存更改
            if(commentEntity.getStatus()==CommentStatus.AUDIT.getValue()){
                if(comment.getStatus()==CommentStatus.NORMAL.getValue()){
                    org.aileme.common.redis.utils.RedisUtils.getClient().getMap(RedisKeys.getPostKey(comment.getPostId()), StringCodec.INSTANCE).addAndGet(ConfigConstant.POST_COMMENT_NUM, lists.size()+1);
                }
            }else{
                //如果是修改审核过的评论，正常处理
                if(comment.getStatus()==CommentStatus.OFF.getValue()){
                    org.aileme.common.redis.utils.RedisUtils.getClient().getMap(RedisKeys.getPostKey(comment.getPostId()), StringCodec.INSTANCE).addAndGet(ConfigConstant.POST_COMMENT_NUM, -(lists.size()+1));
                }else{
                    org.aileme.common.redis.utils.RedisUtils.getClient().getMap(RedisKeys.getPostKey(comment.getPostId()), StringCodec.INSTANCE).addAndGet(ConfigConstant.POST_COMMENT_NUM, lists.size()+1);
                }
            }


        }
        org.aileme.common.redis.utils.RedisUtils.deleteObject(ConfigConstant.COMMENT_KEY+comment.getPostId());
        this.updateById(comment);
    }

    @Override
    public List<AppChildrenCommentResponse> remainComment(Integer id) {
        List<CommentEntity> child = this.getByPid(id);
        if (child.size() >= 2) {
            child.subList(0, 2).clear();
        }
        List<AppChildrenCommentResponse> responseList=new ArrayList<>();
        AppUserEntity user = localUser.getUser();
        child.forEach(l -> {
            AppChildrenCommentResponse childrenResponse = new AppChildrenCommentResponse();
            BeanUtils.copyProperties(l, childrenResponse);
            AppUserEntity commentUser = appUserService.getById(l.getUid());
            AppUserShortInfoResponse userVo=new AppUserShortInfoResponse();
            BeanUtils.copyProperties(commentUser,userVo);
            childrenResponse.setUserInfo(userVo);
            AppUserEntity toUserInfo = appUserService.getById(l.getToUid());
            AppUserShortInfoResponse toUserInfoVo=new AppUserShortInfoResponse();
            BeanUtils.copyProperties(toUserInfo,toUserInfoVo);
            childrenResponse.setToUser(toUserInfoVo);
            childrenResponse.setThumbs(commentThumbsService.getThumbsCount(l.getId()));
            if(user==null){
                childrenResponse.setIsThumbs(false);
            }else{
                childrenResponse.setIsThumbs(commentThumbsService.isThumbs(user.getUid(), l.getId()));
            }
            responseList.add(childrenResponse);
        });
        return responseList;
    }
}
