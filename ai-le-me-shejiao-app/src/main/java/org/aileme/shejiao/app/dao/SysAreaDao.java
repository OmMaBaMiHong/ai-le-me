package org.aileme.shejiao.app.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.aileme.shejiao.domain.entity.sys.SysArea;
/**
 * sys_area 表 DAO 接口（Mapper）
 */
@Mapper
public interface SysAreaDao extends BaseMapper<SysArea> {
    // 继承 BaseMapper 后，自动拥有 CRUD 方法：
    // selectById(Long id)、insert(SysArea entity)、updateById(SysArea entity)、deleteById(Long id) 等
}
