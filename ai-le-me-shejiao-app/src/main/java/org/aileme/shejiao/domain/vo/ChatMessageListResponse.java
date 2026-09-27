package org.aileme.shejiao.domain.vo;

import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.MessageEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author linfeng
 * @date 2022/1/28 16:47
 */
@Data
@Schema(name = "ChatMessageListResponse", description = "私信消息响应体")
public class ChatMessageListResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "数量")
    private Integer count;

    @Schema(description = "用户信息")
    private AppUserEntity userInfo;

    @Schema(description = "消息代表")
    private MessageEntity msg;
}
