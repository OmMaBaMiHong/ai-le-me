package org.aileme.shejiao.domain.param.app;
import lombok.Data;
@Data
public class DeletePostForm {
    private Integer id;
    private Integer isSendDeleteInfo;
    private String deleteReason;
}
