package org.aileme.shejiao.admin.dao;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.aileme.shejiao.domain.vo.ChartDataResponse;
import org.aileme.shejiao.domain.entity.admin.UserRechargeEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;
/**
 * 用户充值
 * 
 * @author linfeng
 * @email linfengtech001@163.com
 * @date 2022-04-19 19:27:33
 */
@Mapper
public interface UserRechargeDao extends BaseMapper<UserRechargeEntity> {


    @Select("select IFNULL(sum(price),0) from pay_order " +
            "where status=1")
    double rechargeMoney();


    @Select("SELECT IFNULL(sum(price),0) " +
            " FROM pay_order ${ew.customSqlSegment}")
    double rechargeMoneyByMonth(@Param(Constants.WRAPPER) Wrapper<UserRechargeEntity> wrapper);

    @Select("SELECT IFNULL(sum(price),0) as num," +
            "DATE_FORMAT(add_time, '%m-%d') as time " +
            " FROM pay_order where status=1 and add_time >= #{time}" +
            " GROUP BY DATE_FORMAT(add_time,'%Y-%m-%d') " +
            " ORDER BY add_time ASC")
    List<ChartDataResponse> chartList(Date nowMonth);
}
