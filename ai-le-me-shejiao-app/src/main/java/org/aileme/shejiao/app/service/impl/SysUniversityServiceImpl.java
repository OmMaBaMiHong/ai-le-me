package org.aileme.shejiao.app.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.SysUniversityService;
import org.aileme.shejiao.app.dao.SysUniversityDao;
import org.aileme.shejiao.domain.entity.sys.SysUniversity;


import java.util.List;

@DS("master")
@Service
public class SysUniversityServiceImpl extends ServiceImpl<SysUniversityDao, SysUniversity> implements SysUniversityService {

    @Autowired
    private SysUniversityDao sysUniversityDao;

    // 1. 根据ID查询高校
    public SysUniversity getById(Long id) {
        return sysUniversityDao.selectById(id);
    }

    // 2. 查询广东省的所有高校（province_id=19）
    public List<SysUniversity> getByProvinceId() {
        return sysUniversityDao.selectByProvinceId(19L);
    }

    // 3. 查询所有985高校
    public List<SysUniversity> get985University() {
        return sysUniversityDao.selectByTag("985");
    }

    // 4. 分页查询本科理工院校
    public Page<SysUniversity> pageQuery(Integer pageNum, Integer pageSize) {
        Page<SysUniversity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUniversity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUniversity::getLevel, 1) // 本科
               .eq(SysUniversity::getType, "理工") // 理工类型
               .eq(SysUniversity::getStatus, 1); // 启用状态
        return sysUniversityDao.selectPage(page, wrapper);
    }

    // 5. 新增高校
    public boolean add(SysUniversity university) {
        return sysUniversityDao.insert(university) > 0;
    }
}
