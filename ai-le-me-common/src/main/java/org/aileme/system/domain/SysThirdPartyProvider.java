package org.aileme.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.aileme.common.mybatis.core.domain.BaseEntity;

/**
 * 第三方服务提供商对象 sys_third_party_provider
 *
 * @author system
 * @date 2026-02-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_third_party_provider")
public class SysThirdPartyProvider extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 提供商ID
     */
    @TableId(type = IdType.AUTO)
    private Long providerId;

    /**
     * 服务类型
     */
    private String serviceType;

    /**
     * 提供商代码
     */
    private String providerCode;

    /**
     * 提供商名称
     */
    private String providerName;

    /**
     * 提供商Logo
     */
    private String providerLogo;

    /**
     * 是否当前使用:0=否,1=是
     */
    private Integer isCurrent;

    /**
     * 是否启用:0=禁用,1=启用
     */
    private Integer isEnabled;

    /**
     * 显示顺序
     */
    private Integer displayOrder;

    /**
     * 官方网站
     */
    private String officialWebsite;

    /**
     * 文档地址
     */
    private String docUrl;

    /**
     * 备注
     */
    private String remark;

    /**
     * 渠道配置JSON
     */
    private String configJson;
}
