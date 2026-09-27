package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data  // Restored for proper compilation
//@Data  // Temporarily commented out due to Maven compilation issue
@Schema(name = "关注")
public class AdminUserInfoForm {

    @Schema(description = "用户id")
    private Integer uid;
    /**
     * 状态
     */
    @Schema(description = "状态")
    private Integer status;

    /**
     * 0为普通用户  1官方账号 2马甲虚拟用户
     */
    @Schema(description = "0为普通用户  1官方账号 2马甲虚拟用户")
    private Integer type;

    /**
     *余额不修改1，修改0
     */
    @Schema(description = "余额不修改1，修改0")
    private Integer changeMoney;
    /**
     *余额 增加0 减少1
     */
    @Schema(description = "余额 增加0 减少1")
    private Integer upOrDown;
    /**
     * 余额增减数量
     */
    @Schema(description = "余额增减数量")
    private BigDecimal changeValue;

    // Added missing getter/setter methods for compilation
    public Integer getUid() {
        return uid;
    }

    public void setUid(Integer uid) {
        this.uid = uid;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getChangeMoney() {
        return changeMoney;
    }

    public void setChangeMoney(Integer changeMoney) {
        this.changeMoney = changeMoney;
    }

    public Integer getUpOrDown() {
        return upOrDown;
    }

    public void setUpOrDown(Integer upOrDown) {
        this.upOrDown = upOrDown;
    }

    public BigDecimal getChangeValue() {
        return changeValue;
    }

    public void setChangeValue(BigDecimal changeValue) {
        this.changeValue = changeValue;
    }
}