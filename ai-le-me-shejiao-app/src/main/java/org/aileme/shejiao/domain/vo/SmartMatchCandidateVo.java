package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "SmartMatchCandidateVo", description = "智能红娘推荐对象")
public class SmartMatchCandidateVo {

    private Integer uid;
    private String username;
    private String avatar;
    private String figur;
    private Integer gender;
    private String city;
    private String locationCity;
    private String abodeCity;
    private Integer age;
    private Integer height;
    private Integer education;
    private String educationText;
    private String job;
    private String intro;
    private String selfIntroduction;
    private String loveDeclaration;
    private String interest;
    private Integer vip;
    private Integer auditStatus;
    private Integer identyCertifStatus;
    private Integer eduCertifStatus;
    private Integer matchScore;
    private String matchSummary;
    @Builder.Default
    private List<String> fitTags = new ArrayList<>();
    @Builder.Default
    private List<String> reasons = new ArrayList<>();
    @Builder.Default
    private List<String> icebreakOpeners = new ArrayList<>();
    private Double openingReadinessScore;
    private String openingStyleHint;
    private String recommendedAction;
    private Boolean autoChatEligible;
    private String doNotOpenReason;
    private String masterStyleCode;
    private String provider;
    private String debugTraceId;
    private Boolean likedByOwner;
    private Boolean likesOwner;
}
