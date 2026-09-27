package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.domain.vo.PostDetailResponse;
import org.aileme.shejiao.domain.vo.PostVipInfoResponse;
import org.aileme.shejiao.domain.vo.TopicPostResponse;
import org.aileme.shejiao.common.utils.AppPageUtils;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.PostEntity;
import org.aileme.shejiao.domain.param.app.*;

import java.util.List;
import java.util.Map;

/**
 *
 *
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-23 20:49:55
 */
public interface PostService extends IService<PostEntity> {

    PageUtils queryPage(Map<String, Object> params);

    Integer findTopicPostCount(Integer topicId);

    List<TopicPostResponse> findTopicPostCountBatch(List<Integer> topicIdList);

    PostDetailResponse detail(Integer id);

    AppPageUtils queryPageList(PostListForm request);

    AppPageUtils getPostListByDiscussId(PostListForm request);

    AppPageUtils getListByTopicId(PostListForm request);

    AppPageUtils getListByUid(PostListForm request);

    Integer addComment(AddCommentForm request, AppUserEntity user);

    void addCollection(AddCollectionForm request, AppUserEntity user);

    AppPageUtils joinTopicPost(Integer page, AppUserEntity user);

    AppPageUtils lastPost(Integer page, Integer classId);

    AppPageUtils followUserPost(Integer page, AppUserEntity user);

    List<String> findThreeMedia(Integer id);

    Long getPostNumberByUid(Integer uid);

    Integer addPost(AddPostForm request, AppUserEntity user);

    AppPageUtils myCollectPost(Integer page,AppUserEntity user);

    AppPageUtils myPost(Integer page, AppUserEntity user);

    AppPageUtils search(Integer page, String keyword);

    void del(Integer id, Integer uid);

    Integer getPostNumberByDiscussId(Integer id);

    Boolean setAdmin(SetAdminForm request, AppUserEntity user);

    Boolean cancelAdmin(SetAdminForm request, AppUserEntity user);


    void deleteByAdmin(List<Integer> integers);

    PostVipInfoResponse getVipPostInfo(VipPostInfoForm request);

    Integer voteAdd(AddVoteForm request, AppUserEntity user);

    void userVote(UserVoteForm request, AppUserEntity user);

    void downByAdmin(DownPostForm param);

    void upByAdmin(List<Integer> list);

    void getRobotPostContent();

    public AppUserEntity virtualUser();

    Boolean setPostTop(SetPostTopForm request, AppUserEntity user);

    Boolean topPostDel(SetPostTopForm request, AppUserEntity user);

    String getSharePic(Integer postId, String origin, String url,AppUserEntity user) throws Exception;

    List<PostEntity> getTopPost();

    List<PostEntity> getHotPost();

    AppPageUtils getPostListByType(Integer page, Integer type);

    Integer addArticle(AddArticleForm request, Integer uid);

    void postAddByAdmin(AddPostByAdminForm request);
    List<String> getImagePostListByUser(Integer uid);

    AppPageUtils queryShortVideoPageList(VideoListForm request);

    void addReadCount(Integer postId);

    void deletePostIdByAdmin(DeletePostForm param);
}
