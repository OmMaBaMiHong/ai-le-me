package org.aileme.shejiao.app.service.agent;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AgentBillingPluginRegistryService {

    private static final String DEFAULT_MODE = "coin";

    private final Map<String, AgentBillingPlugin> plugins = new LinkedHashMap<>();
    private final AgentBillingPlugin defaultPlugin;

    public AgentBillingPluginRegistryService(List<AgentBillingPlugin> pluginList) {
        if (pluginList != null) {
            for (AgentBillingPlugin plugin : pluginList) {
                if (plugin == null || StringUtils.isBlank(plugin.modeCode())) {
                    continue;
                }
                plugins.put(plugin.modeCode().trim().toLowerCase(Locale.ROOT), plugin);
            }
        }
        this.defaultPlugin = plugins.get(DEFAULT_MODE);
    }

    public AgentBillingPlugin resolve(String modeCode) {
        if (StringUtils.isNotBlank(modeCode)) {
            AgentBillingPlugin plugin = plugins.get(modeCode.trim().toLowerCase(Locale.ROOT));
            if (plugin != null) {
                return plugin;
            }
        }
        if (defaultPlugin != null) {
            return defaultPlugin;
        }
        return plugins.values().stream().findFirst().orElseThrow(() -> new IllegalStateException("No billing plugin registered"));
    }
}
