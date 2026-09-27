package org.aileme.shejiao.app.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.PostCollectionService;
import org.aileme.shejiao.app.dao.PostCollectionDao;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.Query;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.app.PostCollectionEntity;
import org.aileme.shejiao.domain.param.app.AddCollectionForm;
import org.aileme.shejiao.domain.vo.PostCountResponse;
import org.aileme.shejiao.domain.vo.PostIsCollectionResponse;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 帖子收藏服务实现类
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-24 20:49:32
 */
@DS("master")
@Service("postCollectionService")
public class PostCollectionServiceImpl extends ServiceImpl<PostCollectionDao, PostCollectionEntity> implements PostCollectionService {

    @Resource
    private PostCollectionDao postCollectionDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PostCollectionEntity> page = this.page(
                new Query<PostCollectionEntity>().getPage(params),
                new QueryWrapper<>()
        );
        return new PageUtils(page);
    }

    @Override
    public Integer collectCount(Integer postId) {
        return Math.toIntExact(this.lambdaQuery()
                .eq(PostCollectionEntity::getPostId, postId)
                .count());
    }

    @Override
    public Boolean isCollection(Integer uid, Integer postId) {
        long count = this.lambdaQuery()
                .eq(PostCollectionEntity::getUid, uid)
                .eq(PostCollectionEntity::getPostId, postId)
                .count();
        return count > 0;
    }

    @Override
    public void cancelCollection(AddCollectionForm request, AppUserEntity user) {
        this.lambdaUpdate()
                .eq(PostCollectionEntity::getUid, user.getUid())
                .eq(PostCollectionEntity::getPostId, request.getId())
                .remove();
    }

    @Override
    public List<Integer> getPostListByUid(Integer uid) {
        return this.lambdaQuery()
                .eq(PostCollectionEntity::getUid, uid)
                .list()
                .stream()
                .map(PostCollectionEntity::getPostId)
                .toList();
    }

    @Override
    public List<PostCountResponse> findBatchCollectCount(List<Integer> list) {
        return postCollectionDao.findBatchCollectCount(list);
    }

    @Override
    public List<PostIsCollectionResponse> findIsCollectBatch(List<Integer> list, Integer uid) {
        return postCollectionDao.findIsCollectBatch(list, uid);
    }
}
