package org.aileme.shejiao.admin.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import org.aileme.shejiao.admin.dao.TagsDao;
import org.aileme.shejiao.api.service.TagsService;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.Query;
import org.aileme.shejiao.domain.entity.admin.TagsEntity;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@DS("master")
@Service("tagsService")
public class TagsServiceImpl extends ServiceImpl<TagsDao, TagsEntity> implements TagsService {

    @Autowired
    private TagsDao tagsDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String key = (String) params.get("key");
        String tagName = (String) params.get("tagName");
        String tagCategory = (String) params.get("tagCategory");
        Integer status = params.get("status") != null ? Integer.valueOf(params.get("status").toString()) : null;

        QueryWrapper<TagsEntity> wrapper = new QueryWrapper<>();
        // 兼容管理端传入的通用关键词查询 key
        if (key != null && !key.isEmpty()) {
            wrapper.and(w -> w.like("tag_name", key).or().like("tag_category", key));
        } else {
            if (tagName != null && !tagName.isEmpty()) {
                wrapper.like("tag_name", tagName);
            }
            if (tagCategory != null && !tagCategory.isEmpty()) {
                wrapper.eq("tag_category", tagCategory);
            }
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByAsc("tag_category").orderByAsc("sort");

        IPage<TagsEntity> page = this.page(new Query<TagsEntity>().getPage(params), wrapper);
        return new PageUtils(page);
    }

    @Override
    @DSTransactional
    public void saveTag(TagsEntity tag) {
        // 检查标签名是否重复
        long count = this.lambdaQuery()
                .eq(TagsEntity::getTagName, tag.getTagName())
                .count();
        if (count > 0) {
            throw new LinfengException("标签名已存在");
        }

        tag.setUsageCount(0);
        tag.setCreateTime(new Date());
        tag.setUpdateTime(new Date());
        this.save(tag);
    }

    @Override
    @DSTransactional
    public void updateTag(TagsEntity tag) {
        // 检查标签名是否与其他标签冲突
        long count = this.lambdaQuery()
                .eq(TagsEntity::getTagName, tag.getTagName())
                .ne(TagsEntity::getId, tag.getId())
                .count();
        if (count > 0) {
            throw new LinfengException("标签名已被其他标签使用");
        }

        tag.setUpdateTime(new Date());
        this.updateById(tag);
    }

    @Override
    public List<TagsEntity> getByCategory(String tagCategory) {
        return tagsDao.getByCategory(tagCategory);
    }

    @Override
    public List<TagsEntity> getBatchByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return tagsDao.getBatchByIds(ids);
    }

    @Override
    public List<String> getAllCategories() {
        return this.lambdaQuery()
                .select(TagsEntity::getTagCategory)
                .groupBy(TagsEntity::getTagCategory)
                .list()
                .stream()
                .map(TagsEntity::getTagCategory)
                .collect(Collectors.toList());
    }

    @Override
    @DSTransactional
    public void increaseUsageCount(Integer tagId) {
        tagsDao.increaseUsageCount(tagId);
    }
}
