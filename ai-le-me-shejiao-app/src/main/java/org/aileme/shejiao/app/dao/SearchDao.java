package org.aileme.shejiao.app.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.aileme.shejiao.domain.entity.app.SearchEntity;

import java.util.List;
/**
 * 用户历史搜索信息表
 *
 * @author JL.Yu
 * @email linfengtech001@163.com
 * @date 2023-04-26 20:13:08
 */
@Mapper
public interface SearchDao extends BaseMapper<SearchEntity> {

    List<String> selectHotSearch();
}
