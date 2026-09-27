package org.aileme.shejiao.domain.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * @author linfeng
 * @date 2022/1/21 17:28
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Schema(title="TopicListResponse对象", description="圈子列表响应对象")
public class TopicListResponse implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 圈子id
     */
    @Schema(title = "圈子id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    /**
     * 创建用户id
     */
    @Schema(title = "创建用户id")
    private Integer uid;
    /**
     * 分类id
     */
    @Schema(title = "分类id")
    private Integer cateId;
    /**
     * 频道类型：1-官方频道 2-主理人频道 3-用户圈子
     */
    @Schema(title = "频道类型：1-官方频道 2-主理人频道 3-用户圈子")
    private Integer channelType;
    /**
     * 关联主理人ID
     */
    @Schema(title = "关联主理人ID")
    private Integer hongniangId;
    /**
     * 圈子名称
     */
    @Schema(title = "圈子名称")
    private String topicName;
    /**
     * 描述
     */
    @Schema(title = "描述")
    private String description;
    /**
     * logo
     */
    @Schema(title = "logo")
    private String coverImage;
    /**
     * 背景图
     */
    @Schema(title = "背景图")
    private String bgImage;
    /**
     * 推荐类型：0 不推荐， 1首页推荐， 2圈子页推荐
     */
    @Schema(title = "推荐类型：0 不推荐， 1首页推荐， 2圈子页推荐")
    private Integer topType;
    /**
     * 圈子状态：0 正常 ，1禁用
     */
    @Schema(title = "圈子状态：0 正常 ，1禁用")
    private Integer status;
    /**
     * 是否首页推荐圈子内容
     */
    @Schema(title = "是否首页推荐圈子内容")
    private Integer indexRecommend;
    /**
     * 加入人数
     */
    @Schema(title = "加入人数")
    private Integer userNum;
    /**
     * 创建时间
     */
    @Schema(title = "创建时间")
    private Date createTime;

    @Schema(title = "用户信息")
    private AppUserEntity userInfo;

    @Schema(title = "关联主理人详情")
    private HongniangTopicInfoResponse hongniangInfo;

    /**
     * 圈子的内容数量
     */
    @Schema(title = "圈子的内容数量")
    private Integer postCount;
    /**
     * 已加入圈子的人数
     */
    @Schema(title = "已加入圈子的人数")
    private Integer userCount;

    /**
     * 是否官方圈子
     */
    @Schema(title = "是否官方圈子")
    private Boolean isOfficial;


}
