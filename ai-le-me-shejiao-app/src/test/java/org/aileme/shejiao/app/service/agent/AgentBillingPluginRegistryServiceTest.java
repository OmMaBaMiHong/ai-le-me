package org.aileme.shejiao.app.service.agent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AgentBillingPluginRegistryServiceTest {

    @Test
    public void resolveReturnsRegisteredPluginAndFallsBackToCoinPlugin() {
        AgentBillingPlugin coinPlugin = mock(AgentBillingPlugin.class);
        AgentBillingPlugin logicOnlyPlugin = mock(AgentBillingPlugin.class);
        when(coinPlugin.modeCode()).thenReturn("coin");
        when(logicOnlyPlugin.modeCode()).thenReturn("logic_only");

        AgentBillingPluginRegistryService registry = new AgentBillingPluginRegistryService(List.of(coinPlugin, logicOnlyPlugin));

        assertSame(logicOnlyPlugin, registry.resolve("logic_only"));
        assertSame(coinPlugin, registry.resolve("unknown_mode"));
        assertSame(coinPlugin, registry.resolve(null));
    }
}
