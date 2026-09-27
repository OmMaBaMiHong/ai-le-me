package org.aileme.shejiao.app.runtime.compliance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiniAppTotalSwitchSupportTest {

    @Test
    void shouldTreatZeroAsEnabled() {
        assertTrue(MiniAppTotalSwitchSupport.isEnabled("0", false));
    }

    @Test
    void shouldTreatOneAsDisabled() {
        assertFalse(MiniAppTotalSwitchSupport.isEnabled("1", true));
    }

    @Test
    void shouldUseEnabledDefaultWhenConfigMissing() {
        assertTrue(MiniAppTotalSwitchSupport.isEnabled("", true));
        assertFalse(MiniAppTotalSwitchSupport.isEnabled(null, false));
    }

    @Test
    void shouldFallbackToEnabledStorageValueWhenConfigMissing() {
        assertEquals("0", MiniAppTotalSwitchSupport.normalizeValue("", true));
    }
}
