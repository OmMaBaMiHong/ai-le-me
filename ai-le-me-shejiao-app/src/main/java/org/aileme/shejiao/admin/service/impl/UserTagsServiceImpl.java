package org.aileme.shejiao.admin.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import org.aileme.shejiao.admin.dao.UserTagsDao;
import org.aileme.shejiao.api.service.TagsService;
import org.aileme.shejiao.api.service.UserTagsService;
import org.aileme.shejiao.domain.entity.admin.TagsEntity;
import org.aileme.shejiao.domain.entity.admin.UserTagsEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@DS("master")
@Service("userTagsService")
public class UserTagsServiceImpl extends ServiceImpl<UserTagsDao, UserTagsEntity> implements UserTagsService {

    @Autowired
    private UserTagsDao userTagsDao;

    @Autowired
    private TagsService tagsService;

    @Override
    public List<Integer> getTagIdsByUserId(Integer userId) {
        return userTagsDao.getTagIdsByUserId(userId);
    }

    @Override
    public List<TagsEntity> getTagsByUserId(Integer userId) {
        List<Integer> tagIds = getTagIdsByUserId(userId);
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        return tagsService.getBatchByIds(tagIds);
    }

    @Override
    public List<Integer> getUserIdsByTagId(Integer tagId) {
        return userTagsDao.getUserIdsByTagId(tagId);
    }

    @Override
    @DSTransactional
    public void batchInsert(List<UserTagsEntity> list) {
        if (list != null && !list.isEmpty()) {
            userTagsDao.batchInsert(list);
        }
    }

    @Override
    @DSTransactional
    public void setUserTags(Integer userId, List<Integer> tagIds, Integer sourceType) {
        // 先删除旧标签
        deleteByUserId(userId);

        // 插入新标签
        if (tagIds != null && !tagIds.isEmpty()) {
            List<UserTagsEntity> list = new ArrayList<>();
            for (Integer tagId : tagIds) {
                UserTagsEntity userTag = new UserTagsEntity();
                userTag.setUserId(userId);
                userTag.setTagId(tagId);
                userTag.setSourceType(sourceType);
                userTag.setWeight(BigDecimal.ONE);
                userTag.setCreateTime(new Date());
                list.add(userTag);
            }
            batchInsert(list);

            // 更新标签使用次数
            for (Integer tagId : tagIds) {
                tagsService.increaseUsageCount(tagId);
            }
        }
    }

    @Override
    @DSTransactional
    public void deleteByUserId(Integer userId) {
        userTagsDao.deleteByUserId(userId);
    }
}
