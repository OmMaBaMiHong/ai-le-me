package org.aileme.shejiao.app.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.VideoTemplateService;
import org.aileme.shejiao.app.dao.VideoTemplateDao;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.Query;
import org.aileme.shejiao.domain.entity.app.VideoTemplateEntity;

import java.util.Map;

/**
 * 视频模板服务实现
 *
 * @author system
 * @date 2026-02-13
 */
@DS("master")
@Service("videoTemplateService")
public class VideoTemplateServiceImpl extends ServiceImpl<VideoTemplateDao, VideoTemplateEntity> implements VideoTemplateService {

    @Override
    public VideoTemplateEntity getByCode(String code) {
        return this.lambdaQuery()
                .eq(VideoTemplateEntity::getCode, code)
                .eq(VideoTemplateEntity::getStatus, VideoTemplateEntity.STATUS_ENABLED)
                .one();
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String name = params.get("name") != null ? params.get("name").toString() : null;
        Integer status = params.get("status") != null ? Integer.valueOf(params.get("status").toString()) : null;
        String category = params.get("category") != null ? params.get("category").toString() : null;

        QueryWrapper<VideoTemplateEntity> wrapper = new QueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like("name", name);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        if (category != null && !category.isEmpty()) {
            wrapper.eq("category", category);
        }
        wrapper.orderByAsc("sort").orderByDesc("create_time");

        IPage<VideoTemplateEntity> page = this.page(new Query<VideoTemplateEntity>().getPage(params), wrapper);
        return new PageUtils(page);
    }
}
