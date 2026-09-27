package org.aileme.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.aileme.common.mybatis.core.domain.BaseEntity;

/**
 * 第三方服务路由规则对象 sys_third_party_route_rule
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_third_party_route_rule")
public class SysThirdPartyRouteRule extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long routeRuleId;

    private String serviceType;

    private String sceneCode;

    private String templateCode;

    private String contentMode;

    private String functionType;

    private String providerCode;

    private String profileCode;

    private String matchJson;

    private Integer priority;

    private Integer isEnabled;

    private String remark;
}
