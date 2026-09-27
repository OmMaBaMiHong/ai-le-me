package org.aileme.shejiao.app.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.NameChangeService;
import org.aileme.shejiao.app.dao.NameChangeDao;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.Query;
import org.aileme.shejiao.domain.entity.app.NameChangeEntity;

import jakarta.annotation.Resource;
import java.util.Map;

@DS("master")
@Service("nameChangeService")
public class NameChangeServiceImpl extends ServiceImpl<NameChangeDao, NameChangeEntity> implements NameChangeService {

    @Resource
    private NameChangeDao nameChangeDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<NameChangeEntity> page = this.page(
                new Query<NameChangeEntity>().getPage(params),
                new QueryWrapper<>()
        );
        return new PageUtils(page);
    }

    @Override
    public boolean canChangeName(org.aileme.shejiao.domain.entity.admin.AppUserEntity user) {
        // 检查用户是否可以修改名称
        long count = this.lambdaQuery()
                .eq(NameChangeEntity::getUid, user.getUid())
                .count();
        return count == 0;
    }
}
