package org.aileme.shejiao.domain.entity.sys;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 全国省市区行政区划表 DO
 * 自动生成的实体类，对应 sys_area 表
 */
@Data
@TableName("sys_region") // 关联数据库表名
public class SysArea implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 行政区划代码（主键）
     */
    @TableId(type = IdType.INPUT) // 主键类型为手动输入（非自增），对应表中PRIMARY KEY (`code`)
    private Integer code;

    /**
     * 行政区划名称
     */
    private String name;

    /**
     * 上级区划代码（驼峰：pcode → pCode）
     */
    private Integer pcode;

    /**
     * 地名简称（驼峰：sname → sName）
     */
    private String sname;

    /**
     * 行政区划等级（1：省、直辖市；2：市州；3：区县）
     */
    private Integer level;

    /**
     * 组合名称（驼峰：mername → merName）
     */
    private String mername;

    /**
     * 拼音
     */
    private String pinyin;
}