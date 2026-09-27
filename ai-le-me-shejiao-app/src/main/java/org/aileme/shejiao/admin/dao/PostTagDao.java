package org.aileme.shejiao.admin.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.aileme.shejiao.domain.entity.admin.PostTagEntity;

/**
 * 帖子话题关联Dao
 */
@Mapper
public interface PostTagDao extends BaseMapper<PostTagEntity> {
}
