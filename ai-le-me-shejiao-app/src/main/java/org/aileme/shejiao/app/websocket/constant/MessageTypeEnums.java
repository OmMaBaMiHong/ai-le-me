package org.aileme.shejiao.app.websocket.constant;

import java.util.Arrays;

public enum MessageTypeEnums {

    PERSON_MESSAGE("person-message","私聊") ,
    PERSON_WITHDRAW( "person-withdraw","撤回私聊消息") ,
    PERSON_APPLY("person-apply","好友申请"),
    PERSON_APPLY_AGREE("person-apply-agree","好友申请通过"),
    AI_MSG("ai-message","ai消息"),
    AI_DIALOGUE("ai-dialogue","ai对话")
    ;


    MessageTypeEnums(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    private String type;
    private String desc;

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    public static  MessageTypeEnums messageTypeEnums(String type){
        return Arrays.stream(MessageTypeEnums.values()).filter(f->f.getType().equals(type)).findFirst().orElse(null);
    }
}
