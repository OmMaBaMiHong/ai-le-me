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

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.Query;

import org.aileme.shejiao.admin.dao.VoteSubjectDao;
import org.aileme.shejiao.domain.entity.admin.VoteSubjectEntity;
import org.aileme.shejiao.api.service.VoteSubjectService;


@DS("master")
@Service("voteSubjectService")
public class VoteSubjectServiceImpl extends ServiceImpl<VoteSubjectDao, VoteSubjectEntity> implements VoteSubjectService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<VoteSubjectEntity> page = this.page(
                new Query<VoteSubjectEntity>().getPage(params),
                new QueryWrapper<VoteSubjectEntity>()
        );

        return new PageUtils(page);
    }

}
