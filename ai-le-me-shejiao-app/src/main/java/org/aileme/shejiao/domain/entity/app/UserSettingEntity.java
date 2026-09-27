package org.aileme.shejiao.domain.entity.app;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 用户隐私设置表
 * 
 * @author JL.Yu
 * @email linfengtech001@163.com
 * @date 2022-11-24 15:17:15
 */
@Data
@Entity
@Table(name = "user_setting")
@TableName("user_setting")
public class UserSettingEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主键ID
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@TableId
	private Integer id;
	/**
	 * 用户ID
	 */
	private Integer uid;
	/**
	 * 隐藏粉丝 0否1是
	 */
	private Integer isFollow;
	/**
	 * 隐藏关注 0否1是
	 */
	private Integer isWatch;
	/**
	 * 隐藏作品 0否1是
	 */
	private Integer isPost;
	/**
	 * 在线状态 0单身,1=母胎,2=热恋中,3=官宣领证,4.分手了
	 * private Integer myLive;
	 */

}
