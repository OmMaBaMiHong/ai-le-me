package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.domain.vo.TopicDetailResponse;
import org.aileme.shejiao.domain.vo.TopicListResponse;
import org.aileme.shejiao.common.utils.AppPageUtils;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.TopicEntity;
import org.aileme.shejiao.domain.param.app.*;

import java.util.List;
import java.util.Map;

/**
 *
 *
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-21 17:01:12
 */
public interface TopicService extends IService<TopicEntity> {

    PageUtils queryPage(Map<String, Object> params);

    AppPageUtils queryByPage(Map<String, Object> params);

    TopicDetailResponse detail(Integer id);

    Integer joinTopic(Integer id, AppUserEntity user);

    void userTopicDel(Integer id, AppUserEntity user);

    void topicDel(Integer id, AppUserEntity user);

    void topicDeleteByAdmin(List<Integer> list);

    AppPageUtils userJoinTopic(UserJoinTopicForm request, AppUserEntity user);

    AppPageUtils queryByPageList(Map<String,Object> params);

    List<TopicListResponse> hotTopic();

    AppPageUtils classTopicAreImg(Integer classId, Integer page);

    AppPageUtils myCreateTopic(Integer page,AppUserEntity user);

    Integer topicAdd(TopicAddForm topic,AppUserEntity user);

    AppPageUtils search(Integer page, String keyword);

    Boolean topicEdit(TopicUpdateForm topic, AppUserEntity user);

    boolean detection(Integer uid);

    String getQrCode(Integer topicId, String origin, String url,AppUserEntity user) throws Exception ;

    Boolean giveTopic(SetAdminForm request, AppUserEntity user);

    void updateByAdmin(TopicEntity topic);

    List<TopicEntity> getJoinTopicList(Integer id);

    void doBlock(BlockForm param, AppUserEntity user);

    void removeBlock(BlockForm param, AppUserEntity user);

    void joinTopicApply(UserJoinTopicApplyForm request, AppUserEntity user);

    Map<Integer,String> getAllByList(List<Integer> topicIdList);

    void saveTopicByAdmin(TopicEntity topic);

    boolean privateCirclesOpen();
}
