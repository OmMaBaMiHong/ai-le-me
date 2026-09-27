package org.aileme.shejiao.domain.param.app;

/**
 * 收入状态
 */
public enum IncomeEnums {
    ONE_LEVEL(1,"10-20万/年"),
    TWO_LEVEL(2,"20-30万/年"),
    THREE_LEVEL(3,"30-40万/年"),
    FOUR_LEVEL(4,"40-50万/年"),
    FiVE_LEVEL(5,"50万以上/年"),
    ;
    private int code;
    private String desc;

    IncomeEnums(int code, String desc) {
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
