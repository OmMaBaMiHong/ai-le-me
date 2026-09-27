package org.aileme.shejiao.domain.param.app;

public enum FollowEnums {
    TO_FOLLOW  (0,"关注"),
    MUTUAL_FOLLOW  (1,"互相关注"),
    FOLLOWED (2,"已关注"),
    NO_LOVE(3,"不喜欢"),
    ;
    private Integer code;
    private String desc;

    FollowEnums(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
