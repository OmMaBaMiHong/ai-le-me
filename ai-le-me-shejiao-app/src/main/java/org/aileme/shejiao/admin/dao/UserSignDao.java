package org.aileme.shejiao.admin.dao;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.aileme.shejiao.domain.vo.SignResponse;
import org.aileme.shejiao.domain.entity.admin.UserSignEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
/**
 * 签到记录表
 * 
 * @author linfeng
 * @email linfengtech001@163.com
 * @date 2022-05-07 15:01:03
 */
@Mapper
public interface UserSignDao extends BaseMapper<UserSignEntity> {

    @Select("SELECT DATE_FORMAT(a.add_time,'%Y-%m-%d') as addTime,a.title,a.number " +
            "FROM account_bill a  WHERE a.category = 'integral'" +
            " AND a.type = 'sign' AND a.status = 1 AND a.uid = #{uid} " +
            "ORDER BY a.add_time DESC")
    List<SignResponse> getSignList(@Param("uid") Integer uid, Page page);

}
