package org.aileme.shejiao.api.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import org.aileme.shejiao.admin.dao.PostTagDao;
import org.aileme.shejiao.api.service.PostTagService;
import org.aileme.shejiao.domain.entity.admin.PostTagEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 帖子话题关联服务实现
 */
@Service
public class PostTagServiceImpl extends ServiceImpl<PostTagDao, PostTagEntity> implements PostTagService {
    
    @Override
    @DSTransactional
    public void savePostTags(Integer postId, List<Integer> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        
        // 先删除旧关联
        this.lambdaUpdate()
            .eq(PostTagEntity::getPostId, postId)
            .remove();
        
        // 批量插入新关联
        List<PostTagEntity> entities = tagIds.stream()
            .map(tagId -> {
                PostTagEntity entity = new PostTagEntity();
                entity.setPostId(postId);
                entity.setTagId(tagId);
                entity.setCreateTime(LocalDateTime.now());
                return entity;
            })
            .collect(Collectors.toList());
        
        this.saveBatch(entities);
    }
    
    @Override
    public List<Integer> getTagIdsByPostId(Integer postId) {
        return this.lambdaQuery()
            .eq(PostTagEntity::getPostId, postId)
            .list()
            .stream()
            .map(PostTagEntity::getTagId)
            .collect(Collectors.toList());
    }
    
    @Override
    public void deleteByPostId(Integer postId) {
        this.lambdaUpdate()
            .eq(PostTagEntity::getPostId, postId)
            .remove();
    }
}
