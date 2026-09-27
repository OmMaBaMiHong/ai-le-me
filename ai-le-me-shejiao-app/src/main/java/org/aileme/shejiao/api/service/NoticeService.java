/**
 * -----------------------------------
 *  Copyright (c) 2021-2023

 * 版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.app.NoticeEntity;
import org.aileme.shejiao.domain.param.app.ReadNoticeForm;
import org.aileme.shejiao.domain.param.app.RejectNoticeForm;

import java.util.Map;

/**
 * IM通知
 *
 * @author JL.Yu
 * @email linfengtech001@163.com
 * @date 2022-11-16 15:04:19
 */
public interface NoticeService extends IService<NoticeEntity> {

    PageUtils queryPage(Map<String, Object> params);

    Map<String,Object> getNoticeList(Integer uid);

    void readById(ReadNoticeForm param);

    void reject(RejectNoticeForm param);
}

