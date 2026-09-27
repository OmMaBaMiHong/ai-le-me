package org.aileme.shejiao.app.dao;

import org.aileme.shejiao.domain.vo.PostCountResponse;
import org.aileme.shejiao.domain.vo.PostIsCollectionResponse;
import org.aileme.shejiao.domain.entity.app.PostCollectionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
/**
 * 
 * 
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-24 20:49:32
 */
@Mapper
public interface PostCollectionDao extends BaseMapper<PostCollectionEntity> {

    List<PostCountResponse> findBatchCollectCount(List<Integer> list);

    List<PostIsCollectionResponse> findIsCollectBatch(@Param("list") List<Integer> list, @Param("uid") Integer uid);
}
