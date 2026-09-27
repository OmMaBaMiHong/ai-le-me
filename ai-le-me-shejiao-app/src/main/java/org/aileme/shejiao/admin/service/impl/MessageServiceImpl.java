/**
 * -----------------------------------
 *  Copyright (c) 2021-2023
 *  All rights reserved, Designed By my.hots.love
 *  
 *  商业版授权联系技术客服	 QQ:  3582996245
 *  严禁分享、盗用、转卖源码或非法牟利！
 *  版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.admin.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import org.aileme.shejiao.common.utils.*;
import org.aileme.shejiao.admin.utils.WechatUtil;import org.aileme.shejiao.domain.vo.MessageListResponse;
import org.aileme.shejiao.domain.vo.MessageNumberResponse;
import org.aileme.shejiao.admin.dao.MessageDao;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.MessageEntity;
import org.aileme.shejiao.domain.entity.admin.PostEntity;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.api.service.MessageService;
import org.aileme.shejiao.api.service.PostService;
import org.aileme.shejiao.domain.param.app.MessageReadForm;
import org.aileme.shejiao.domain.param.app.UpdateChatStatusForm;
import org.aileme.shejiao.domain.param.app.UpdateSystemNoticeStatusForm;
import org.aileme.shejiao.admin.utils.LocalUser;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;


@DS("master")
@Service("messageService")
public class MessageServiceImpl extends ServiceImpl<MessageDao, MessageEntity> implements MessageService {

    @Autowired
    private AppUserService appUserService;
    @Lazy
    @Autowired
    private PostService postService;
    @Autowired
    private LocalUser localUser;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<MessageEntity> queryWrapper=new QueryWrapper<>();
        //条件查询
        String key = (String) params.get("key");
        String type = (String) params.get("type");
        if (!WechatUtil.isEmpty(key)) {
            queryWrapper.lambda().like(MessageEntity::getTitle,key);
        }
        if (!WechatUtil.isEmpty(type)) {
            queryWrapper.lambda().eq(MessageEntity::getType,Integer.valueOf(type));
        }
        queryWrapper.lambda().orderByDesc(MessageEntity::getMId);
        IPage<MessageEntity> page = this.page(
                new Query<MessageEntity>().getPage(params),
                queryWrapper
        );
        return new PageUtils(page);
    }

    @Override
    public MessageNumberResponse getMessageNumber() {
        AppUserEntity user = localUser.getUser();
        if(ObjectUtil.isNull(user)){
            MessageNumberResponse response=new MessageNumberResponse();
            response.setThumbCount(0);
            response.setComment(0);
            response.setAllCount(0);
            response.setFollow(0);
            response.setChatMsgList(new ArrayList<>());
            response.setArticleMsgList(new ArrayList<>());
            return response;
        }
        MessageNumberResponse response=new MessageNumberResponse();
        Integer allCount = Math.toIntExact(this.lambdaQuery()
                .eq(MessageEntity::getStatus, Constant.NOT_READ)
                .eq(MessageEntity::getToUid, user.getUid())
                .count());
        Integer follow = Math.toIntExact(this.lambdaQuery()
                .eq(MessageEntity::getStatus, Constant.NOT_READ)
                .eq(MessageEntity::getToUid, user.getUid())
                .eq(MessageEntity::getType, Constant.WATCH)
                .count());
        Integer comment = Math.toIntExact(this.lambdaQuery()
                .eq(MessageEntity::getStatus, Constant.NOT_READ)
                .eq(MessageEntity::getToUid, user.getUid())
                .eq(MessageEntity::getType, Constant.COMMENT)
                .count());
        Integer thumbCollect = Math.toIntExact(this.lambdaQuery()
                .eq(MessageEntity::getStatus, Constant.NOT_READ)
                .eq(MessageEntity::getToUid, user.getUid())
                .and(wrapper->wrapper.eq(MessageEntity::getType,Constant.STAR).or().eq(MessageEntity::getType,Constant.COLLECT))
                .count());
        //系统通知消息查询
        Integer systemUnreadCount = Math.toIntExact(this.lambdaQuery().eq(MessageEntity::getType, Constant.PUSHARTICLE)
                .eq(MessageEntity::getToUid, user.getUid())
                .eq(MessageEntity::getStatus, Constant.NOT_READ)
                .orderByDesc(MessageEntity::getMId)
                .count());
        response.setAllCount(allCount);
        response.setFollow(follow);
        response.setComment(comment);
        response.setThumbCount(thumbCollect);
        response.setSystemUnreadCount(systemUnreadCount);
        response.setArticleMsgList(new ArrayList<>());
        response.setChatMsgList(new ArrayList<>());
        return response;
    }

    /**
     * 查询消息列表
     * 1为点赞，2为评论  3为收藏 4为关注  5为推送文章 6私聊
     */
    @Override
    public AppPageUtils queryMessageList(Integer type, Integer currPage,AppUserEntity user) {
        LambdaQueryWrapper<MessageEntity> queryWrapper = new LambdaQueryWrapper<>();
        List<Integer> list = new ArrayList<>();
        list.add(Constant.STAR);
        list.add(Constant.COLLECT);
        queryWrapper.eq(MessageEntity::getToUid,user.getUid());
        if(type==1){
            queryWrapper.in(MessageEntity::getType,list);
        }else if(type==2){
            queryWrapper.eq(MessageEntity::getType,Constant.WATCH);
        }else if(type==3){
            queryWrapper.eq(MessageEntity::getType,Constant.COMMENT);
        }else if(type==5){
            queryWrapper.eq(MessageEntity::getType,Constant.PUSHARTICLE);
        }else if(type==6){
            queryWrapper.eq(MessageEntity::getType,Constant.CHAT);
        }
        queryWrapper.orderByDesc(MessageEntity::getMId);
        Page<MessageEntity> page = new Page<>(currPage, 10);
        Page<MessageEntity> page1 = this.page(page, queryWrapper);
        AppPageUtils pages = new AppPageUtils(page1);
        List<MessageEntity> messageList = (List<MessageEntity>) pages.getData();
        List<MessageListResponse> responseList=new ArrayList<>();
        messageList.forEach(l->{
            AppUserEntity appUserEntity= appUserService.getById(l.getFromUid());
            if(appUserEntity!=null){
                MessageListResponse response=new MessageListResponse();
                BeanUtils.copyProperties(l,response);
                response.setUserInfo(appUserEntity);
                if(!l.getType().equals(Constant.WATCH)){
                    PostEntity postEntity= postService.getById(l.getPostId());
                    if(postEntity!=null){
                        response.setPostContent(postEntity.getContent());
                    }
                }
                response.setPostContent("");
                responseList.add(response);
            }

        });
        pages.setData(responseList);
        return pages;
    }

    /**
     * 消息异步发送
     * @param fromUid 发送者uid
     * @param toUid  接收者uid
     * @param postId 帖子id
     * @param type 1为点赞，2为评论  3为收藏 4为关注  5为推送文章 6私聊
     * @param content  发送内容
     * @param title 发送标题
     */
    public void sendMessage(Integer fromUid,Integer toUid,Integer postId,Integer type,String content,String title){
        if(fromUid.equals(toUid)){
            return;
        }
        MessageEntity message=new MessageEntity();
        message.setContent(content);
        message.setPostId(postId);
        message.setFromUid(fromUid);
        message.setTitle(title);
        message.setCreateTime(DateUtil.nowDateTime());
        message.setToUid(toUid);
        message.setType(type);
        message.setStatus(Constant.NOT_READ);
        this.save(message);
    }

    /**
     * 消息同步发送 用于用户消息私聊等
     * @param fromUid 发送者uid
     * @param toUid  接收者uid
     * @param postId 帖子id
     * @param type 1为点赞，2为评论  3为收藏 4为关注  5为推送文章 6私聊
     * @param content  发送内容
     * @param title 发送标题
     */
    @DSTransactional
    public void sendMessageNotAsync(Integer fromUid,Integer toUid,Integer postId,Integer type,String content,String title){
        if(fromUid.equals(toUid)){
            return;
        }
        MessageEntity message=new MessageEntity();
        message.setContent(content);
        message.setPostId(postId);
        message.setFromUid(fromUid);
        message.setTitle(title);
        message.setCreateTime(DateUtil.nowDateTime());
        message.setToUid(toUid);
        message.setType(type);
        message.setStatus(Constant.NOT_READ);
        this.save(message);
    }

    @Override
    public Boolean status(Integer type, Integer uid) {

        UpdateWrapper<MessageEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("status",Constant.HAS_READ);
        updateWrapper.eq("to_uid",uid);
        if(type==1){
            updateWrapper.and(wrapper->wrapper.eq("type",Constant.STAR).or().eq("type",Constant.COLLECT));
        }else if(type==2){
            updateWrapper.eq("type",Constant.WATCH);
        }else if(type==3){
            updateWrapper.eq("type",Constant.COMMENT);
        }else if(type==5){
            updateWrapper.eq("type",Constant.PUSHARTICLE);
        }else if(type==6){
            updateWrapper.eq("type",Constant.CHAT);
        }
        return update(updateWrapper);
    }



    @Override
    public Boolean articleMsgState(UpdateSystemNoticeStatusForm request, Integer uid) {
        return this.lambdaUpdate()
                .set(MessageEntity::getStatus,Constant.HAS_READ)
                .eq(MessageEntity::getFromUid,0)
                .eq(MessageEntity::getToUid,uid)
                .eq(MessageEntity::getPostId,request.getId())
                .eq(MessageEntity::getType,Constant.PUSHARTICLE)
                .update();
    }

    /**
     * 修改用户点击的消息为已读状态
     * @param request
     * @param uid
     */
    @Override
    public void readMessage(MessageReadForm request, Integer uid) {
        this.lambdaUpdate()
                .set(MessageEntity::getStatus,Constant.HAS_READ)
                .eq(MessageEntity::getToUid,uid)
                .eq(MessageEntity::getPostId,request.getPostId())
                .eq(MessageEntity::getMId,request.getMid())
                .update();
    }

    /**
     * 修改关注的人的消息为已读状态
     * @param uid
     */
    @Override
    public void readAllWatchInfo(Integer uid) {
        List<MessageEntity> list = this.lambdaQuery()
                .eq(MessageEntity::getType, Constant.WATCH)
                .eq(MessageEntity::getToUid, uid)
                .eq(MessageEntity::getStatus, Constant.NOT_READ).list();
        list.forEach(l-> l.setStatus(Constant.HAS_READ));
        updateBatchById(list);

    }

    /**
     * 清除几个月前的消息数据
     * @param month
     */
    @Override
    public void deleteMessageByMonth(Integer month) {
        String s = DateUtils.addDateMonths(new Date(),-month);
        LambdaQueryWrapper<MessageEntity> wrapper=new LambdaQueryWrapper<>();
        wrapper.le(MessageEntity::getCreateTime,s);
        this.remove(wrapper);
    }

    /**
     * 删除不重要消息（除私信外的其他消息）
     * 如果你有业务需求需要保留别的类型消息，就在这里更改
     * @param day day天前的消息
     */
    @Override
    public void deleteSomeMessageByDay(Integer day) {
        String s = DateUtils.addDateDays(new Date(), -day);
        LambdaQueryWrapper<MessageEntity> wrapper=new LambdaQueryWrapper<>();
        wrapper.ne(MessageEntity::getType,Constant.CHAT);
        wrapper.le(MessageEntity::getCreateTime,s);
        this.remove(wrapper);
    }

    @Override
    public AppPageUtils getSystemList(Integer toUid, Integer fromUid,Integer currPage) {

        LambdaQueryWrapper<MessageEntity> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(MessageEntity::getToUid,fromUid);
        queryWrapper.eq(MessageEntity::getType, Constant.PUSHARTICLE);
        queryWrapper.orderByDesc(MessageEntity::getMId);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<MessageEntity> page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(currPage, 10);//每次获取的数量
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<MessageEntity> pages=this.page(page,queryWrapper);
        return new AppPageUtils(pages);
    }

    @Override
    public void updateSystemStatus(UpdateChatStatusForm request, Integer uid) {
        this.lambdaUpdate()
                .set(MessageEntity::getStatus, Constant.HAS_READ)
                .eq(MessageEntity::getToUid, uid)
                .eq(MessageEntity::getType, Constant.PUSHARTICLE)
                .update();
    }

    @Override
    public void delSystemMsg(Integer uid) {
        LambdaQueryWrapper<MessageEntity> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(MessageEntity::getToUid,uid);
        queryWrapper.eq(MessageEntity::getType,Constant.PUSHARTICLE);
        queryWrapper.eq(MessageEntity::getFromUid,0);
        this.remove(queryWrapper);
    }

    @Override
    public boolean deleteMessageById(Integer mid, Integer uid) {
        if (mid == null || uid == null) {
            return false;
        }
        return this.lambdaUpdate()
                .eq(MessageEntity::getMId, mid)
                .eq(MessageEntity::getToUid, uid)
                .remove();
    }
}
