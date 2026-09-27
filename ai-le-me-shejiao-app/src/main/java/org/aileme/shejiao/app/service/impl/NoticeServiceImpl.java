package org.aileme.shejiao.app.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.NoticeService;
import org.aileme.shejiao.app.dao.NoticeDao;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.Query;
import org.aileme.shejiao.domain.entity.app.NoticeEntity;
import org.aileme.shejiao.domain.param.app.ReadNoticeForm;
import org.aileme.shejiao.domain.param.app.RejectNoticeForm;

import java.util.HashMap;
import java.util.Map;

@DS("master")
@Service("noticeService")
public class NoticeServiceImpl extends ServiceImpl<NoticeDao, NoticeEntity> implements NoticeService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<NoticeEntity> page = this.page(
                new Query<NoticeEntity>().getPage(params),
                new QueryWrapper<>()
        );
        return new PageUtils(page);
    }

    @Override
    public Map<String, Object> getNoticeList(Integer uid) {
        return new HashMap<>();
    }

    @Override
    public void readById(ReadNoticeForm param) {
        // 简化实现
    }

    @Override
    public void reject(RejectNoticeForm param) {
        // 简化实现
    }
}
