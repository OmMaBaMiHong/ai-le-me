package org.aileme.shejiao.domain.entity.app;

import com.baomidou.mybatisplus.annotation.IdType;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.Version;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * ai应用
 * </p>
 *
 * @author lww
 * @since 2023-06-10
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(name="AiApp对象", description="导航栏表")
public class AiBot implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
      @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer uid;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "预设提示词")
    private String prePrompt;

    @Schema(description = "图片")
    private String img;

    @Schema(description = "跳转路径")
    private String url;

    @Schema(description = "跳转类型0页面1外链")
    private Integer type;

    @Schema(description = "状态0正常1禁用")
    private Integer status;

    @Schema(description = "创建时间")
      @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @Schema(description = "更新时间")
      @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    private String model;
    private String info;
    private String category;
}