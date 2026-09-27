package org.aileme.shejiao.common.enums;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Arrays;

@Schema(title = "学历枚举")
public enum EduEnum {

    DEFAULT_EDU(0,"未知"),
    DA_ZHUAN(1,"大专"),
    BEN_KE(2,"本科"),
    SHUO_SHI(3,"硕士"),
    BO_SHI(4,"博士"),
    BO_SHI_HOU(5,"博士后"),



    ;
    private Integer value;
    private String desc;

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    EduEnum(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }
    public static EduEnum getBYDesc(String desc){
       return   Arrays.stream(EduEnum.values()).filter(f->f.getDesc().equals(desc)).findFirst().orElse(DEFAULT_EDU);
    }
}
