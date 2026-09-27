package org.aileme.shejiao.app.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.aileme.shejiao.domain.entity.sys.SysUniversity;

import java.util.List;
/**
 * sys_university 表 DAO 接口（Mapper）
 * 继承 BaseMapper 后自动拥有基础 CRUD 方法，扩展常用业务查询
 */
@Mapper // 标记为MyBatis Mapper接口
public interface SysUniversityDao extends BaseMapper<SysUniversity> {

    /**
     * 基础CRUD（继承BaseMapper已包含，无需手动写）：
     * - selectById(Long id)：根据ID查询
     * - insert(SysUniversity entity)：新增
     * - updateById(SysUniversity entity)：根据ID更新
     * - deleteById(Long id)：根据ID删除
     * - selectList(Wrapper<SysUniversity> queryWrapper)：条件查询列表
     */

    // ========== 扩展常用业务查询（自定义SQL） ==========

    /**
     * 根据省份ID查询高校列表
     * @param provinceId 省份ID（关联sys_area表省级ID）
     * @return 该省份的所有高校
     */
    @Select("SELECT * FROM sys_university WHERE province_id = #{provinceId} AND status = 1 ORDER BY uni_name")
    List<SysUniversity> selectByProvinceId(@Param("provinceId") Long provinceId);

    /**
     * 根据特色标签查询高校（如985、211、双一流）
     * @param tag 标签（如：985）
     * @return 符合标签的高校列表
     */
    @Select("SELECT * FROM sys_university WHERE tag LIKE CONCAT('%', #{tag}, '%') AND status = 1 ORDER BY uni_name")
    List<SysUniversity> selectByTag(@Param("tag") String tag);
//
//    /**
//     * 分页查询高校（支持条件筛选）
//     * @param page 分页参数
//     * @param queryWrapper 条件构造器
//     * @return 分页结果
//     */
//    IPage<SysUniversity> selectPage(IPage<SysUniversity> page, @Param(Constants.WRAPPER) Wrapper<SysUniversity> queryWrapper);

    /**
     * 根据办学层次+学校类型查询高校
     * @param level 办学层次（1=本科，2=专科）
     * @param type 学校类型（如：理工、综合）
     * @return 符合条件的高校列表
     */
    @Select("SELECT * FROM sys_university WHERE level = #{level} AND type = #{type} AND status = 1 ORDER BY uni_name")
    List<SysUniversity> selectByLevelAndType(@Param("level") Integer level, @Param("type") String type);
}
