package org.aileme.shejiao.app.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.MomentService;
import org.aileme.shejiao.app.dao.MomentDao;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.Query;
import org.aileme.shejiao.domain.entity.app.MomentEntity;

import java.util.Map;

@DS("master")
@Service("momentService")
public class MomentServiceImpl extends ServiceImpl<MomentDao, MomentEntity> implements MomentService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MomentEntity> page = this.page(
                new Query<MomentEntity>().getPage(params),
                new QueryWrapper<>()
        );
        return new PageUtils(page);
    }
}
