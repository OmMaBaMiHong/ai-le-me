package org.aileme.shejiao.domain.entity.admin;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 敏感词库信息表
 * 
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-28 13:40:57
 */
@Data
@Entity
@Table(name = "os_sensitive")
@TableName("os_sensitive")
public class SensitiveEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@TableId
	private Integer id;
	/**
	 * 敏感词库
	 */
	private String sensitiveWord;
	/**
	 * 是否开启 1-是 0-否
	 */
	private Integer state;
	/**
	 * 处理措施  1-禁止发布 2-需审核
	 */
	private Integer handleMeasures;

}
