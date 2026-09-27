package org.aileme.shejiao.app.service.agent;

public final class AgentGovernanceConstants {

    private AgentGovernanceConstants() {
    }

    public static final String CAPABILITY_PHOTO_READ_SELECTED = "photo.read.selected";
    public static final String CAPABILITY_COMPANION_ENABLED = "companion.enabled";
    public static final String CAPABILITY_MESSAGE_DRAFT = "message.draft";
    public static final String CAPABILITY_MESSAGE_AUTO_SEND = "message.auto.send";
    public static final String CAPABILITY_GIFT_PLAN = "gift.plan";
    public static final String CAPABILITY_GIFT_EXECUTE = "gift.execute";
    public static final String CAPABILITY_MOMENT_DRAFT = "moment.draft";
    public static final String CAPABILITY_MOMENT_PUBLISH = "moment.publish";
    public static final String CAPABILITY_DATE_COORDINATE = "date.coordinate";
    public static final String CAPABILITY_FOLLOW_RECOMMEND = "follow.recommend";

    public static final String AUTHORIZE_MODE_MANUAL_ONLY = "manual_only";
    public static final String AUTHORIZE_MODE_AUTO_DRAFT = "auto_draft";
    public static final String AUTHORIZE_MODE_CONDITIONAL_AUTO = "conditional_auto";

    public static final String TARGET_SCOPE_NONE = "none";
    public static final String TARGET_SCOPE_ALL = "all";
    public static final String TARGET_SCOPE_WHITELIST = "whitelist";
    public static final String TARGET_SCOPE_FRIENDS = "friends";
    public static final String TARGET_SCOPE_MATCHED_ONLY = "matched_only";
    public static final String TARGET_SCOPE_CUSTOM = "custom";

    public static final String RISK_LEVEL_LOW = "low";
    public static final String RISK_LEVEL_MEDIUM = "medium";
    public static final String RISK_LEVEL_HIGH = "high";
    public static final String RISK_LEVEL_CRITICAL = "critical";

    public static final String TASK_STATUS_DRAFT = "draft";
    public static final String TASK_STATUS_PENDING_APPROVAL = "pending_approval";
    public static final String TASK_STATUS_APPROVED = "approved";
    public static final String TASK_STATUS_RUNNING = "running";
    public static final String TASK_STATUS_SUCCEEDED = "succeeded";
    public static final String TASK_STATUS_FAILED = "failed";
    public static final String TASK_STATUS_REJECTED = "rejected";
    public static final String TASK_STATUS_CANCELLED = "cancelled";
    public static final String TASK_STATUS_EXPIRED = "expired";

    public static final String APPROVAL_STATUS_PENDING = "pending";
    public static final String APPROVAL_STATUS_APPROVED = "approved";
    public static final String APPROVAL_STATUS_REJECTED = "rejected";
    public static final String APPROVAL_STATUS_EXPIRED = "expired";
    public static final String APPROVAL_STATUS_CANCELLED = "cancelled";
}
