
package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;

@Data
@Schema(name = "私聊消息请求体")
public class ChatSendForm {

    @Schema(description = "接收用户uid")
    private Integer uid;

    @NotBlank
    @Length(max = 100, message = "消息不能超过100个字符")
    @Schema(description = "content")
    private String content;

}
