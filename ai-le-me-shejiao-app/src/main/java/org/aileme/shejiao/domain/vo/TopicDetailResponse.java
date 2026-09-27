package org.aileme.shejiao.domain.vo;

import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.DiscussEntity;
import org.aileme.shejiao.domain.entity.admin.PostEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author linfeng
 * @date 2022/1/23 17:28
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Schema(name = "TopicDetailResponse", description = "圈子详情响应对象")
public class TopicDetailResponse implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 圈子id
     */
    @Schema(description = "圈子id")
    private Integer id;
    /**
     * 创建用户id
     */
    @Schema(description = "创建用户id")
    private Integer uid;
    /**
     * 分类id
     */
    @Schema(description = "分类id")
    private Integer cateId;
    /**
     * 频道类型：1-官方频道 2-主理人频道 3-用户圈子
     */
    @Schema(description = "频道类型：1-官方频道 2-主理人频道 3-用户圈子")
    private Integer channelType;
    /**
     * 关联主理人ID
     */
    @Schema(description = "关联主理人ID")
    private Integer hongniangId;
    /**
     * 圈子名称
     */
    @Schema(description = "圈子名称")
    private String topicName;
    /**
     * 描述
     */
    @Schema(description = "描述")
    private String description;
    /**
     * logo
     */
    @Schema(description = "logo")
    private String coverImage;
    /**
     * 背景图
     */
    @Schema(description = "背景图")
    private String bgImage;
    /**
     * 推荐类型：0 不推荐， 1首页推荐， 2圈子页推荐
     */
    @Schema(description = "推荐类型：0 不推荐， 1首页推荐， 2圈子页推荐")
    private Integer topType;
    /**
     * 圈子状态：0 正常 ，1禁用
     */
    @Schema(description = "圈子状态：0 正常 ，1禁用")
    private Integer status;
    /**
     * 是否首页推荐圈子内容
     */
    @Schema(description = "是否首页推荐圈子内容")
    private Integer indexRecommend;
    /**
     * 加入人数
     */
    @Schema(description = "加入人数")
    private Integer userNum;
    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createTime;

    /**
     * 用户详情
     */
    @Schema(description = "用户详情")
    private AppUserShortInfoResponse userInfo;

    @Schema(description = "关联主理人详情")
    private HongniangTopicInfoResponse hongniangInfo;


    /**
     * 圈子的内容数量
     */
    @Schema(description = "圈子的内容数量")
    private Integer postCount;
    /**
     * 已加入圈子的人数
     */
    @Schema(description = "已加入圈子的人数")
    private Integer userCount;

    /**
     * 圈子的管理员
     */
    @Schema(description = "圈子的管理员")
    private List<AppUserShortInfoResponse> adminList;

    /**
     * 是否为圈主或管理员
     */
    @Schema(description = "是否为圈主或管理员")
    private Boolean isAdmin;

    /**
     * 圈子话题
     */
    @Schema(description = "圈子话题")
    private List<DiscussEntity> discussList;
    /**
     * 是否加入
     */
    @Schema(description = "是否加入")
    private Boolean isJoin;
    /**
     * 置顶内容
     */
//    private List<TopicTopEntity> topPost;
    @Schema(description = "置顶内容")
    private List<PostEntity> topPost;
    /**
     * 圈子用户列表
     */
    @Schema(description = "圈子用户列表")
    private List<AppUserShortInfoResponse> userList;

    /**
     * 是否官方圈子
     */
    @Schema(description = "是否官方圈子")
    private Boolean isOfficial;

}
