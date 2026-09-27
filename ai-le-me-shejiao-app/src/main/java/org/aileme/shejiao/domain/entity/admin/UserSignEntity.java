package org.aileme.shejiao.domain.entity.admin;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 签到记录表
 * 
 * @author linfeng
 * @email linfengtech001@163.com
 * @date 2022-05-07 15:01:03
 */
@Data
@Entity
@Table(name = "user_sign")
@TableName("user_sign")
public class UserSignEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主键id
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@TableId
	private Integer id;
	/**
	 * 用户id
	 */
	private Integer uid;
	/**
	 * 签到说明
	 */
	private String title;
	/**
	 * 获得积分
	 */
	private Integer number;
	/**
	 * 剩余积分
	 */
	private Integer balance;
	/**
	 * 创建时间
	 */
	private Date createTime;

}
