package org.aileme.shejiao.domain.param.app;

import java.util.Arrays;

/**
 * 积分消耗类型配置
 */
public enum IntegralTypeEnum {
    NO_TYPE(0,0,"无"),

    NOTE_TYPE(1,10,"小纸条消耗积分");
    private int codeType;
    private int integray;
    private String desc;

    public int getCodeType() {
        return codeType;
    }

    public void setCodeType(int codeType) {
        this.codeType = codeType;
    }

    public int getIntegray() {
        return integray;
    }

    public void setIntegray(int integray) {
        this.integray = integray;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    IntegralTypeEnum(int codeType, int integray, String desc) {
        this.codeType = codeType;
        this.integray = integray;
        this.desc = desc;
    }
    public  static   IntegralTypeEnum getBytype(int codeType){
        return Arrays.stream(IntegralTypeEnum.values()).filter(f->f.getCodeType()==codeType).findFirst().orElse(IntegralTypeEnum.NO_TYPE);
    }
}
