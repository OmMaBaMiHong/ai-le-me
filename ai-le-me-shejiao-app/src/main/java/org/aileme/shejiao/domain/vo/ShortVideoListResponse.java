package org.aileme.shejiao.domain.vo;
import lombok.Data;
import java.util.List;
@Data
public class ShortVideoListResponse extends PostListResponse {
    private String _id;
    private Boolean isplay;
    private Boolean playIng;
    private String src;
    private String state;
    private List<String> mediasList;
}
