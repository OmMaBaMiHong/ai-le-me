package org.aileme.shejiao.domain.param.app;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
@Data
public class AddPostByAdminForm {
    private Integer uid;
    private Integer topicId;
    private Integer discussId;
    private Integer voteId;
    private String title;
    private String content;
    private List<String> media;
    private Integer readCount;
    private Integer postTop;
    private Integer type;
    private String address;
    private Double longitude;
    private Double latitude;
    private Integer status;
    private Integer cut;
    private BigDecimal pay;
    private String brief;
    private Integer isPrivate;
}
