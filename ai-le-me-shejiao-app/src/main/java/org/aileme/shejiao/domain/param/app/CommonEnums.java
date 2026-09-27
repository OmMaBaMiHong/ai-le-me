package org.aileme.shejiao.domain.param.app;

public enum CommonEnums {
    YES(0,"是"),
    NO(1,"否");

    private int code;
    private String desc;

    CommonEnums(int code, String desc) {
        this.code = code;
        this.desc = desc;
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
