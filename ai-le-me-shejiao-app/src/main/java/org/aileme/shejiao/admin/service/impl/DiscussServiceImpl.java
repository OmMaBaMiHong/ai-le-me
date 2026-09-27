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

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.domain.vo.DiscussDetailResponse;
import org.aileme.shejiao.common.utils.*;
import org.aileme.shejiao.admin.utils.WechatUtil;import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.PostEntity;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.api.service.PostService;
import org.aileme.shejiao.domain.param.app.DiscussAddForm;
import org.aileme.shejiao.domain.param.app.DiscussDeleteForm;
import org.aileme.shejiao.domain.param.app.DiscussListForm;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.aileme.shejiao.admin.dao.DiscussDao;
import org.aileme.shejiao.domain.entity.admin.DiscussEntity;
import org.aileme.shejiao.api.service.DiscussService;


@DS("master")
@Service("discussService")
public class DiscussServiceImpl extends ServiceImpl<DiscussDao, DiscussEntity> implements DiscussService {


    @Autowired
    private AppUserService appUserService;

    @Autowired
    private PostService postService;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<DiscussEntity> page = this.page(
                new Query<DiscussEntity>().getPage(params),
                new QueryWrapper<>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<DiscussEntity> getListByTopicId(Integer topicId) {
        QueryWrapper<DiscussEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(DiscussEntity::getTopicId, topicId);
        Page<DiscussEntity> pageModel = new Page<>(1, 5);
        IPage<DiscussEntity> pageList = this.baseMapper.selectPage(pageModel, wrapper);
        return pageList.getRecords();
    }

    @Override
    public AppPageUtils getDiscussList(DiscussListForm request) {
        QueryWrapper<DiscussEntity> wrapper = new QueryWrapper<>();
        if(request.getTopicId()!=null){
            wrapper.lambda().eq(DiscussEntity::getTopicId, request.getTopicId());
        }
        Page<DiscussEntity> page = new Page<>(request.getPage(),10);
        Page<DiscussEntity> pages = baseMapper.selectPage(page,wrapper);

        return new AppPageUtils(pages);
    }

    @Override
    public AppPageUtils myDiscuss(DiscussListForm request,AppUserEntity user) {
        QueryWrapper<DiscussEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("uid",user.getUid());
        Page<DiscussEntity> page = new Page<>(request.getPage(),10);
        Page<DiscussEntity> pages = baseMapper.selectPage(page,wrapper);
        return new AppPageUtils(pages);
    }

    @Override
    public DiscussDetailResponse detail(Integer id) {
        DiscussEntity discuss = this.getById(id);
        if(discuss==null){
            throw new LinfengException("该话题不存在");
        }
        discuss.setReadCount(discuss.getReadCount()+1);
        this.updateById(discuss);
        DiscussDetailResponse response=new DiscussDetailResponse();
        BeanUtils.copyProperties(discuss,response);
        AppUserEntity appUser = appUserService.getById(discuss.getUid());
        if(!WechatUtil.isEmpty(appUser.getMobile())){
            appUser.setMobile(WechatUtil.maskMobile(appUser.getMobile()));
        }
        response.setUserInfo(appUser);
        response.setPostCount(postService.getPostNumberByDiscussId(id));
        return response;
    }

    @Override
    public void deleteDiscuss(DiscussDeleteForm request, AppUserEntity user) {
        DiscussEntity discuss = this.getById(request.getId());
        if(!discuss.getUid().equals(user.getUid())){
            throw new LinfengException("您不是话题创建者");
        }
        List<PostEntity> list = postService.lambdaQuery().eq(PostEntity::getDiscussId, discuss.getId()).list();
        if(list.size()>0){
            throw new LinfengException("该话题存在帖子未删除");
        }
        this.removeById(request.getId());
    }

    @Override
    public Boolean addDiscuss(DiscussAddForm request, AppUserEntity user) {
        DiscussEntity discuss=new DiscussEntity();
        BeanUtils.copyProperties(request,discuss);
        discuss.setCreateTime(DateUtil.nowDateTime());
        discuss.setUid(user.getUid());
        return this.save(discuss);
    }

    /**
     * 查询近一个月浏览量最高的话题
     * @return
     */
    @Override
    public List<DiscussEntity> discussList() {
        DateTime dateTime = cn.hutool.core.date.DateUtil.lastMonth();
        return this.lambdaQuery()
                .gt(DiscussEntity::getCreateTime, dateTime)
                .orderByDesc(DiscussEntity::getReadCount)
                .last("limit 7")
                .list();
    }

    @Override
    public String getDiscussNameById(Integer id) {
        DiscussEntity discuss = this.getById(id);
        if(ObjectUtil.isNull(discuss)){
            return "";
        }
        return discuss.getTitle();
    }

}
