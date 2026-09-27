package org.aileme.shejiao.app.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.PostFabulousService;
import org.aileme.shejiao.app.dao.PostFabulousDao;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.Query;
import org.aileme.shejiao.domain.entity.app.PostFabulousEntity;

import java.util.Map;

/**
 * 帖子点赞服务实现类
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-24 20:49:32
 */
@DS("master")
@Service("postFabulousService")
public class PostFabulousServiceImpl extends ServiceImpl<PostFabulousDao, PostFabulousEntity> implements PostFabulousService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PostFabulousEntity> page = this.page(
                new Query<PostFabulousEntity>().getPage(params),
                new QueryWrapper<>()
        );
        return new PageUtils(page);
    }

    @Override
    public Boolean isThumb(Integer uid, Integer id) {
        long count = this.lambdaQuery()
                .eq(PostFabulousEntity::getUid, uid)
                .eq(PostFabulousEntity::getPostId, id)
                .count();
        return count > 0;
    }
}
