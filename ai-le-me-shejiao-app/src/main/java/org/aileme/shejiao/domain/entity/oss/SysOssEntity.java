
package org.aileme.shejiao.domain.entity.oss;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;


/**
 * 文件上传
 *
 */
@Data
@Entity
@Table(name = "sys_oss")
@TableName("sys_oss")
public class SysOssEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@TableId
	private Long id;
	//URL地址
	private String url;
	//创建时间
	private Date createDate;

}
