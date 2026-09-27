package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(name = "OnboardingPersonaDraftVO", description = "首登关系画像草稿")
public class OnboardingPersonaDraftVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "关系目标")
    private String relationshipGoal;

    @Schema(description = "关系节奏")
    private String romancePace;

    @Schema(description = "沟通风格")
    private String communicationStyle;

    @Schema(description = "情绪需求")
    private List<String> emotionalNeeds = new ArrayList<>();

    @Schema(description = "吸引偏好")
    private List<String> attractionPreferences = new ArrayList<>();

    @Schema(description = "风险点")
    private List<String> riskFlags = new ArrayList<>();

    @Schema(description = "红娘偏好风格")
    private String preferredMatchmakerStyle;

    @Schema(description = "推荐破冰风格")
    private String recommendedOpeningStyle;

    @Schema(description = "推荐标签")
    private List<String> tagCandidates = new ArrayList<>();

    @Schema(description = "画像标题")
    private String archetypeTitle;

    @Schema(description = "画像副标题")
    private String archetypeSubtitle;

    @Schema(description = "核心洞察")
    private List<String> coreInsights = new ArrayList<>();

    @Schema(description = "系统服务策略")
    private List<String> recommendedApproach = new ArrayList<>();

    @Schema(description = "需要避免的信号")
    private List<String> avoidSignals = new ArrayList<>();

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "资料预填草稿")
    private ProfileDraft profileDraft = new ProfileDraft();

    @Data
    @Schema(name = "OnboardingProfileDraft", description = "资料预填草稿")
    public static class ProfileDraft implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "个性签名")
        private String intro;

        @Schema(description = "自我介绍")
        private String selfIntroduction;

        @Schema(description = "爱情观")
        private String loveDeclaration;
    }
}
