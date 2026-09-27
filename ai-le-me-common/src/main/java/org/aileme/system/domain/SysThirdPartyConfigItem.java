package org.aileme.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.aileme.common.mybatis.core.domain.BaseEntity;

/**
 * 第三方服务配置传输对象
 *
 * @author system
 * @date 2026-02-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysThirdPartyConfigItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 配置ID
     */
    @TableId(type = IdType.AUTO)
    private Long configId;

    /**
     * 服务类型:ai/oss/sms/payment/realname/education/push/map
     */
    private String serviceType;

    /**
     * 提供商:tongyi/zhipu/tencent/aliyun等
     */
    private String provider;

    /**
     * 配置键
     */
    private String configKey;

    /**
     * 配置值
     */
    private String configValue;

    /**
     * 默认值
     */
    private String defaultValue;

    /**
     * 值类型:text/password/number/boolean/select/json
     */
    private String valueType;

    /**
     * select类型的可选项(JSON数组)
     */
    private String selectOptions;

    /**
     * 是否必填:0=否,1=是
     */
    private Integer isRequired;

    /**
     * 是否敏感:0=否,1=是(脱敏显示)
     */
    private Integer isSensitive;

    /**
     * 是否启用:0=禁用,1=启用
     */
    private Integer isEnabled;

    /**
     * 显示顺序
     */
    private Integer displayOrder;

    /**
     * 配置项显示名称
     */
    private String configLabel;

    /**
     * 帮助文本/获取地址
     */
    private String helpText;

    /**
     * 备注
     */
    private String remark;
}
