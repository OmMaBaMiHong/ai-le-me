/**
 * -----------------------------------
 * Copyright (c) 2021-2023
 * All rights reserved, Designed By my.hots.love
 * 
 * 商业版授权联系技术客服	 QQ:  3582996245
 * 严禁分享、盗用、转卖源码或非法牟利！
 * 版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.admin.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.domain.vo.AppReportListResponse;
import org.aileme.shejiao.domain.vo.ReportListResponse;
import org.aileme.shejiao.common.utils.*;
import org.aileme.shejiao.admin.utils.WechatUtil;import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.api.service.MessageService;
import org.aileme.shejiao.domain.param.app.ReportAddForm;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.aileme.shejiao.admin.dao.ReportDao;
import org.aileme.shejiao.domain.entity.admin.ReportEntity;
import org.aileme.shejiao.api.service.ReportService;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;


@DS("master")
@Service("reportService")
public class ReportServiceImpl extends ServiceImpl<ReportDao, ReportEntity> implements ReportService {

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private MessageService messageService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {

        QueryWrapper<ReportEntity> queryWrapper = new QueryWrapper<>();
        //条件查询
        String key = (String) params.get("key");
        String type = (String) params.get("type");
        if (NumberUtil.isInteger(key)) {
            if (!WechatUtil.isEmpty(key)) {
                queryWrapper.lambda().eq(ReportEntity::getUid, Integer.parseInt(key));
            }
        } else {
            if (!WechatUtil.isEmpty(key)) {
                queryWrapper.lambda().like(ReportEntity::getContent, key);
            }
        }
        if (!WechatUtil.isEmpty(type)) {
            queryWrapper.lambda().eq(ReportEntity::getStatus, Integer.parseInt(type));
        }
        queryWrapper.lambda().orderByDesc(ReportEntity::getId);
        IPage<ReportEntity> page = this.page(
                new Query<ReportEntity>().getPage(params),
                queryWrapper
        );
        List<ReportEntity> records = page.getRecords();
        List<ReportListResponse> responseList = new ArrayList<>();
        records.forEach(l -> {
            ReportListResponse response = new ReportListResponse();
            BeanUtils.copyProperties(l, response);
            response.setUserInfo(appUserService.getById(response.getUid()));
            response.setMedia(JsonUtils.JsonToList(l.getMedia()));
            responseList.add(response);
        });
        PageUtils pageUtils = new PageUtils(page);
        pageUtils.setList(responseList);
        return pageUtils;
    }

    /**
     * 用户提交举报
     *
     * @param request
     * @param user
     */
    @Override
    @DSTransactional
    public void addReport(ReportAddForm request, AppUserEntity user) {
        Integer count = this.getUserAddReportCount();
        if (count > 10) {
            throw new LinfengException("今日举报次数已达上限！");
        }
        ReportEntity report = new ReportEntity();
        String media = JSON.toJSONString(request.getMedia());
        report.setContent(request.getContent());
        report.setType(request.getCateId());
        report.setLinkId(request.getLinkId());
        report.setMedia(media);
        report.setCreateTime(DateUtil.nowDateTime());
        report.setUpdateTime(DateUtil.nowDateTime());
        report.setUid(user.getUid());
        report.setStatus(Constant.REPORT_REVIEW);
        boolean save = this.save(report);
        if (!save) {
            throw new LinfengException("举报提交失败");
        }
    }

    /**
     * 管理员处理举报单
     *
     * @param report
     */
    @Override
    public void dealByAdmin(ReportEntity report) {
        this.updateById(report);
        if (report.getStatus().equals(Constant.REPORT_FINISH)) {
            String content = StrUtil.format(Constant.REPORT_NOTICE, report.getId(), report.getFeedback());
            messageService.sendMessage(0, report.getUid(), 0, Constant.PUSHARTICLE, content, Constant.TITLE_REPORT);
        }
        if (report.getStatus().equals(Constant.REPORT_REFUSE)) {
            String content = StrUtil.format(Constant.REPORT_NOTICE_REFUSE, report.getId(), report.getFeedback());
            messageService.sendMessage(0, report.getUid(), 0, Constant.PUSHARTICLE, content, Constant.TITLE_REPORT);
        }
    }

    /**
     * 用户举报单列表分页
     *
     * @param page
     * @param status
     * @param user
     * @return
     */
    @Override
    public AppPageUtils listByUser(Integer page, Integer status, AppUserEntity user) {
        LambdaQueryWrapper<ReportEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ReportEntity::getUid, user.getUid());
        if (status >= 0) {
            queryWrapper.eq(ReportEntity::getStatus, status);
        }
        Page<ReportEntity> pages = new Page<>(page, 10);
        Page<ReportEntity> selectPage = baseMapper.selectPage(pages, queryWrapper);
        AppPageUtils appPage = new AppPageUtils(selectPage);
        List<ReportEntity> records = selectPage.getRecords();
        List<AppReportListResponse> responseList = new ArrayList<>();
        records.forEach(l -> {
            AppReportListResponse response = new AppReportListResponse();
            BeanUtils.copyProperties(l, response);
            response.setMedia(JsonUtils.JsonToList(l.getMedia()));
            responseList.add(response);
        });
        appPage.setData(responseList);
        return appPage;
    }

    /**
     * 举报单详情
     *
     * @param id
     * @return
     */
    @Override
    public AppReportListResponse detail(Integer id) {
        AppReportListResponse response = new AppReportListResponse();
        ReportEntity report = this.getById(id);
        BeanUtils.copyProperties(report, response);
        response.setMedia(JsonUtils.JsonToList(report.getMedia()));
        return response;
    }

    /**
     * 获取用户今天举报次数
     *
     * @return
     */
    private Integer getUserAddReportCount() {
        Date today = cn.hutool.core.date.DateUtil.beginOfDay(new Date());
        return Math.toIntExact(this.lambdaQuery()
                .ge(ReportEntity::getCreateTime, today)
                .count());
    }
}
