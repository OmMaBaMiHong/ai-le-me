package org.aileme.shejiao.admin.service.group;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ProviderResult {

    private boolean success;

    private String providerTaskId;

    private String summary;

    private String rawResponse;

    private Map<String, Object> data;
}
