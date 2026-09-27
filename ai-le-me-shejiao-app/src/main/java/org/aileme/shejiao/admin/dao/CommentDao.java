package org.aileme.shejiao.admin.dao;

import org.aileme.shejiao.domain.vo.CommentCountResponse;
import org.aileme.shejiao.domain.entity.admin.CommentEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
/**
 * 
 * 
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-24 21:29:22
 */
@Mapper
public interface CommentDao extends BaseMapper<CommentEntity> {

    List<CommentCountResponse> getAllCountByPostId(List<Integer> id);
}
