package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.domain.vo.DiscussDetailResponse;
import org.aileme.shejiao.common.utils.AppPageUtils;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.DiscussEntity;
import org.aileme.shejiao.domain.param.app.DiscussAddForm;
import org.aileme.shejiao.domain.param.app.DiscussDeleteForm;
import org.aileme.shejiao.domain.param.app.DiscussListForm;

import java.util.List;
import java.util.Map;

/**
 * 
 *
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-24 13:44:37
 */
public interface DiscussService extends IService<DiscussEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<DiscussEntity> getListByTopicId(Integer topicId);

    AppPageUtils getDiscussList(DiscussListForm request);

    AppPageUtils myDiscuss(DiscussListForm request, AppUserEntity user);

    DiscussDetailResponse detail(Integer id);

    void deleteDiscuss(DiscussDeleteForm request, AppUserEntity user);

    Boolean addDiscuss(DiscussAddForm request, AppUserEntity user);

    List<DiscussEntity> discussList();

    String getDiscussNameById(Integer id);
}

