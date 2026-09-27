package org.aileme.shejiao.domain.param.app;

/**
 * 审核状态
 */
public enum ApproveEnum {
    YES(0,"是"),
    NO(1,"否");

    private int code;
    private String desc;

    ApproveEnum(int code, String desc) {
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
