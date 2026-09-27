package org.aileme.shejiao.app.runtime.compliance;

import org.apache.commons.lang3.StringUtils;

/**
 * 小程序备案总开关约定：
 * 0 = 开
 * 1 = 关
 */
public final class MiniAppTotalSwitchSupport {

    public static final String ENABLED_VALUE = "0";
    public static final String DISABLED_VALUE = "1";

    private MiniAppTotalSwitchSupport() {
    }

    public static boolean isEnabled(String value, boolean defaultEnabled) {
        if (StringUtils.isBlank(value)) {
            return defaultEnabled;
        }
        String normalized = value.trim().toLowerCase();
        if (ENABLED_VALUE.equals(normalized)) {
            return true;
        }
        if (DISABLED_VALUE.equals(normalized)) {
            return false;
        }
        if ("true".equals(normalized) || "yes".equals(normalized) || "on".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized) || "no".equals(normalized) || "off".equals(normalized)) {
            return false;
        }
        return defaultEnabled;
    }

    public static String normalizeValue(String value, boolean defaultEnabled) {
        return isEnabled(value, defaultEnabled) ? ENABLED_VALUE : DISABLED_VALUE;
    }
}
