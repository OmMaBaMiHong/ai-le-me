package org.aileme.shejiao.domain.entity.admin;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 *
 *
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-23 20:49:55
 */
@Data
@Entity
@Table(name = "post")
@TableName("post")
public class PostEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@TableId(value = "id",type = IdType.AUTO)
	private Integer id;
	/**
	 * 用户id
	 */
	private Integer uid;
	/**
	 * 圈子id
	 */
	private Integer topicId;
	/**
	 * 话题id
	 */
	private Integer discussId;
	/**
	 * 关联活动id
	 */
	private Integer activityId;
	/**
	 * 投票id
	 */
	private Integer voteId;
	/**
	 * 标题
	 */
	private String title;
	/**
	 * 内容
	 */
	@Lob
	@Column(columnDefinition = "TEXT")
	private String content;
	/**
	 * 文件
	 */
	private String media;
	/**
	 * 浏览量
	 */
	private int readCount;
	/**
	 * 置顶
	 */
	private Integer postTop;
	/**
	 * 帖子类型：1 图文 ，2视频 ，3文章，4投票
	 */
	private Integer type;
	/**
	 * 地址名称
	 */
	private String address;
	/**
	 * 经度
	 */
	private Double longitude;
	/**
	 * 纬度
	 */
	private Double latitude;
	/**
	 * 创建时间
	 */
	private Date createTime;
	/**
	 * 0正常 1待审核 2已拒绝
	 */
	private Integer status;
	/**
	 * 0 普通贴  1 付费贴  2 红包贴
	 */
	private Integer cut;
	/**
	 * 付费贴支付金额
	 */
	private BigDecimal pay;
	/**
	 * 付费简介
	 */
	private String brief;
	/**
	 * 关联私密圈:0公开1私密
	 */
	private Integer isPrivate;

	/**
	 * 是否AI生成视频：0-否 1-是
	 */
	private Integer isAiVideo;

	/**
	 * 关联AI视频记录ID
	 */
	private Integer videoId;
}
