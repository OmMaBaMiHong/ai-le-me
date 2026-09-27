package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.domain.entity.admin.PostTagEntity;

import java.util.List;

/**
 * 帖子话题关联服务
 */
public interface PostTagService extends IService<PostTagEntity> {
    
    /**
     * 批量保存帖子话题关联
     */
    void savePostTags(Integer postId, List<Integer> tagIds);
    
    /**
     * 获取帖子的话题ID列表
     */
    List<Integer> getTagIdsByPostId(Integer postId);
    
    /**
     * 删除帖子的所有话题关联
     */
    void deleteByPostId(Integer postId);
}
