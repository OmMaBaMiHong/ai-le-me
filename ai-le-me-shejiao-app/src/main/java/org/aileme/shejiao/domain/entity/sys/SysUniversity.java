package org.aileme.shejiao.domain.entity.sys;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 全国大学信息表 DO
 * 对应数据库表：sys_university
 */
@Data
@TableName("sys_university") // 关联数据库表名
public class SysUniversity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO) // 主键自增
    private Long id;

    /**
     * 学校标识码（教育部国标，唯一）
     */
    private String uniCode;

    /**
     * 学校全称（如：清华大学、北京大学）
     */
    private String uniName;

    /**
     * 学校简称（如：清华、北大）
     */
    private String shortName;

    /**
     * 办学层次：1=本科，2=专科，3=研究生院校
     */
    private Integer level;

    /**
     * 学校类型（综合/理工/师范/医药/农林/财经等）
     */
    private String type;

    /**
     * 特色标签（逗号分隔：985,211,双一流,双一流建设高校）
     */
    private String tag;

    /**
     * 所属省份ID（关联sys_area表省级ID）
     */
    private Long provinceId;

    /**
     * 所属城市ID（关联sys_area表市级ID）
     */
    private Long cityId;

    /**
     * 详细地址
     */
    private String address;

    /**
     * 邮政编码
     */
    private String zipCode;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 学校官网
     */
    private String website;

    /**
     * 状态：1=启用，0=禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}