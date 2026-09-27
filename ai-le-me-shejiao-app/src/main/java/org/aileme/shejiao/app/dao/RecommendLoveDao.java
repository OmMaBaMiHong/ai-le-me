package org.aileme.shejiao.app.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;
import org.aileme.shejiao.domain.entity.app.RecommendLoveEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
/**
 * <p>
 * 用户推荐设置 Mapper 接口
 * </p>
 *
 * @author lww
 * @since 2023-04-29
 */
@Mapper
public interface RecommendLoveDao extends BaseMapper<RecommendLoveEntity> {

    @Update("UPDATE recommend_love SET rec_num = CASE 10 ELSE 5 END ")
    void updateReconUm();
}
