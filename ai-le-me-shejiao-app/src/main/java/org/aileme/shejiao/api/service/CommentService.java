package org.aileme.shejiao.api.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.domain.vo.AppChildrenCommentResponse;
import org.aileme.shejiao.domain.vo.CommentCountResponse;
import org.aileme.shejiao.common.utils.AppPageUtils;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.CommentEntity;
import org.aileme.shejiao.domain.param.app.DelCommentForm;

import java.util.List;
import java.util.Map;

/**
 *
 *
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-24 21:29:22
 */
public interface CommentService extends IService<CommentEntity> {

    PageUtils queryPage(Map<String, Object> params);

    Integer getCountByPostId(Integer id);

    List<CommentCountResponse> getAllCountByPostId(List<Integer> id);

    AppPageUtils queryCommentPage(Integer postId, Integer page);

    List<CommentEntity> getByPid(Integer pid);

    void del(DelCommentForm request, AppUserEntity user);

    void deleteByAdmin(List<Long> list);

    JSONObject getCommentListInCache(Integer postId);

    void updateCommentById(CommentEntity comment);

    List<AppChildrenCommentResponse> remainComment(Integer id);
}

