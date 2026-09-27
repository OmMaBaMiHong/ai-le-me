package org.aileme.shejiao.domain.vo;
import lombok.Getter;
import lombok.Setter;
import org.aileme.shejiao.domain.entity.admin.PostEntity;
import java.util.List;

@Getter
@Setter
public class AdminPostListResponse extends PostEntity {
    private String topicName;
    private String avatar;
    private String discussTitle;
    private Integer collectionCount;
    private Integer commentCount;
    private Object userInfo;
    private List<String> mediasList;
}
