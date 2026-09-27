package org.aileme.shejiao.app.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.UserScansService;
import org.aileme.shejiao.app.dao.UserScansMapper;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.Query;
import org.aileme.shejiao.domain.entity.app.UserScans;

import java.util.Map;

@DS("master")
@Service("userScansService")
public class UserScansServiceImpl extends ServiceImpl<UserScansMapper, UserScans> implements UserScansService {

    @Override
    public void updateScaNums(Integer id) {
        UserScans userScans = this.getById(id);
        if (userScans != null) {
            userScans.setScanNums(userScans.getScanNums() + 1);
            this.updateById(userScans);
        }
    }
}
