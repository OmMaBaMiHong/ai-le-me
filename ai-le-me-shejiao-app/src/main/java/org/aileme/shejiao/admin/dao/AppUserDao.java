package org.aileme.shejiao.admin.dao;

import org.aileme.shejiao.domain.vo.ChartDataResponse;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;
/**
 * 
 * 
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-20 12:10:43
 */
@Mapper
public interface AppUserDao extends BaseMapper<AppUserEntity> {

    List<AppUserEntity> getBatchUser(List<Integer> uid);

    @Select("SELECT count(uid) as num," +
            "DATE_FORMAT(create_time, '%m-%d') as time " +
            " FROM user where status=0 and create_time >= #{time}" +
            " GROUP BY DATE_FORMAT(create_time,'%Y-%m-%d') " +
            " ORDER BY create_time ASC")
    List<ChartDataResponse> chartList(Date nowMonth);


}
