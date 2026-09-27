package org.aileme.shejiao.app.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.aileme.shejiao.domain.entity.app.UserScans;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
/**
 * <p>
 * 个人主页浏览记录 Mapper 接口
 * </p>
 *
 * @author lww
 * @since 2023-05-16
 */
@Mapper
public interface UserScansMapper extends BaseMapper<UserScans> {

    @Update("update user_scans set scanNums=scanNums+1 where id=#{id}")
    void updateScaNums(@Param("id") Integer id);
}
