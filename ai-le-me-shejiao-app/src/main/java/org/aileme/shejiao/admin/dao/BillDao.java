package org.aileme.shejiao.admin.dao;

import org.aileme.shejiao.domain.entity.admin.BillEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
/**
 * 用户账单表
 * 
 * @author linfeng
 * @email linfengtech001@163.com
 * @date 2022-04-20 20:46:48
 */
@Mapper
public interface BillDao extends BaseMapper<BillEntity> {

    @Select("select IFNULL(sum(price),0) from pay_order " +
            "where status=1 and uid=#{uid} and (type is null or type <> 1)")
    double getAllPay(@Param("uid")Integer uid);

    @Select("select IFNULL(sum(number),0) from account_bill " +
            "where category='integral' and pm=0 and status=1 and uid=#{uid} and type <> 'coin_refund'")
    Integer getUsedIntegral(@Param("uid")Integer uid);
}
