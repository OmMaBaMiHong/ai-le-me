package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 主页统计数据对象
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Schema(name = "HomeRateResponse对象", description = "主页统计数据对象")
public class HomeRateResponse implements Serializable {

    private static final long serialVersionUID=1L;

    @Schema(description = "总用户数")
    private Object totalUser;

    @Schema(description = "总帖子数")
    private Object totalPost;

    @Schema(description = "总待审核帖子数")
    private Object totalPostOfReview;

    @Schema(description = "今日新增用户")
    private Object newUserNum;

    @Schema(description = "昨日新增用户")
    private Object yesterdayNewUserNum;

    @Schema(description = "用户充值总金额")
    private double rechargeMoney;

    @Schema(description = "本月用户充值总金额")
    private double rechargeMoneyByMonth;

    @Schema(description = "今日签到用户数量")
    private double userSignCount;




}
