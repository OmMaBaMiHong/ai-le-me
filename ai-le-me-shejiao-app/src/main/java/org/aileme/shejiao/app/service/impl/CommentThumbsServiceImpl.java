package org.aileme.shejiao.app.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.CommentThumbsService;
import org.aileme.shejiao.app.dao.CommentThumbsDao;
import org.aileme.shejiao.common.utils.DateUtil;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.Query;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.app.CommentThumbsEntity;
import org.aileme.shejiao.domain.param.app.AddThumbsForm;

import java.util.Map;

/**
 * 评论点赞服务实现类
 * @author linfeng
 */
@DS("master")
@Service("commentThumbsService")
public class CommentThumbsServiceImpl extends ServiceImpl<CommentThumbsDao, CommentThumbsEntity> implements CommentThumbsService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CommentThumbsEntity> page = this.page(
                new Query<CommentThumbsEntity>().getPage(params),
                new QueryWrapper<>()
        );
        return new PageUtils(page);
    }

    @Override
    public Integer getThumbsCount(Integer id) {
        return Math.toIntExact(this.lambdaQuery()
                .eq(CommentThumbsEntity::getCId, id)
                .count());
    }

    @Override
    public Boolean isThumbs(Integer uid, Integer id) {
        long count = this.lambdaQuery()
                .eq(CommentThumbsEntity::getUid, uid)
                .eq(CommentThumbsEntity::getCId, id)
                .count();
        return count > 0;
    }

    @Override
    public void addThumbs(AddThumbsForm request, AppUserEntity user) {
        CommentThumbsEntity entity = new CommentThumbsEntity();
        entity.setUid(user.getUid());
        entity.setCId(request.getId().intValue());
        entity.setCreateTime(DateUtil.nowDateTime());
        this.save(entity);
    }

    @Override
    public void cancelThumbs(AddThumbsForm request, AppUserEntity user) {
        this.lambdaUpdate()
                .eq(CommentThumbsEntity::getUid, user.getUid())
                .eq(CommentThumbsEntity::getCId, request.getId().intValue())
                .remove();
    }
}
