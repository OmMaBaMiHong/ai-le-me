package org.aileme.shejiao.domain.entity.admin;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 
 * 
 * @author linfeng
 * @email linfengtech001@163.com
 * @date 2022-05-07 20:28:46
 */
@Data
@Entity
@Table(name = "sign_config")
@TableName("sign_config")
public class SignConfigEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@TableId
	private Integer id;
	/**
	 * 天数
	 */
	private String day;
	/**
	 * 积分数
	 */
	private String signNum;
	/**
	 * 排序
	 */
	private Integer sort;

}
