package org.aileme.shejiao.gateway.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.gateway.chat.provider.LlmProviderStrategy;
import org.aileme.shejiao.gateway.config.AiGatewayProperties;
import org.aileme.shejiao.gateway.runtime.ThirdPartyRouteConfigService;
import org.aileme.shejiao.gateway.runtime.ThirdPartyResolvedRoute;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * OpenAI兼容协议聊天网关
 */
@Service
public class OpenAiCompatibleChatModelGatewayService implements ChatModelGatewayService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleChatModelGatewayService.class);
    private static final LlmProviderStrategy FALLBACK_STRATEGY = new FallbackStrategy();

    private final ThirdPartyRouteConfigService routeConfigService;
    private final AiGatewayProperties properties;
    private final Environment environment;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Map<String, LlmProviderStrategy> providerStrategyRegistry;

    public OpenAiCompatibleChatModelGatewayService(ThirdPartyRouteConfigService routeConfigService,
                                                   AiGatewayProperties properties,
                                                   Environment environment,
                                                   ObjectMapper objectMapper,
                                                   List<LlmProviderStrategy> providerStrategies) {
        this.routeConfigService = routeConfigService;
        this.properties = properties;
        this.environment = environment;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(Math.max(5, properties.getTimeoutSeconds())))
            .build();
        this.providerStrategyRegistry = new LinkedHashMap<>();
        if (providerStrategies != null) {
            for (LlmProviderStrategy strategy : providerStrategies) {
                registerProviderStrategy(strategy.providerCode(), strategy);
                if (strategy.aliases() != null) {
                    for (String alias : strategy.aliases()) {
                        registerProviderStrategy(alias, strategy);
                    }
                }
            }
        }
    }

    @Override
    public String currentProviderCode() {
        String provider = normalizeProvider(routeConfigService.getCurrentProviderCode("ai", properties.getDefaultAiProvider()));
        if (StringUtils.isBlank(provider) || "mock".equals(provider)) {
            provider = normalizeProvider(properties.getDefaultAiProvider());
        }
        return StringUtils.defaultIfBlank(provider, "openai_codex");
    }

    @Override
    public String complete(String scene, String systemPrompt, String userPrompt, Integer maxTokens, Double temperature) {
        return complete(scene, systemPrompt, userPrompt, maxTokens, temperature, properties.getDefaultAiProvider(), null);
    }

    @Override
    public String complete(String scene,
                           String systemPrompt,
                           String userPrompt,
                           Integer maxTokens,
                           Double temperature,
                           String fallbackProvider) {
        return complete(scene, systemPrompt, userPrompt, maxTokens, temperature, fallbackProvider, null);
    }

    @Override
    public String complete(String scene,
                           String systemPrompt,
                           String userPrompt,
                           Integer maxTokens,
                           Double temperature,
                           String fallbackProvider,
                           String preferredModel) {
        if (StringUtils.isAnyBlank(systemPrompt, userPrompt)) {
            return "";
        }

        ThirdPartyResolvedRoute primaryRoute = routeConfigService.resolveRoute(
            "ai",
            buildRouteContext(scene),
            StringUtils.defaultIfBlank(fallbackProvider, properties.getDefaultAiProvider())
        );
        List<String> providers = buildProviderCandidates(primaryRoute.getProviderCode(), fallbackProvider);

        if (StringUtils.isNotBlank(primaryRoute.getProviderCode()) && !"mock".equals(normalizeProvider(primaryRoute.getProviderCode()))) {
            try {
                String response = callProvider(primaryRoute, scene, systemPrompt, userPrompt, maxTokens, temperature, preferredModel);
                if (StringUtils.isNotBlank(response)) {
                    return response;
                }
            } catch (Exception ex) {
                log.warn("[ai-gateway] provider={} profile={} scene={} 调用失败: {}",
                    primaryRoute.getProviderCode(),
                    primaryRoute.getProfileCode(),
                    scene,
                    ex.getMessage());
            }
        }

        for (String provider : providers) {
            if ("mock".equals(provider) || provider.equals(normalizeProvider(primaryRoute.getProviderCode()))) {
                continue;
            }
            try {
                String response = callProvider(provider, scene, systemPrompt, userPrompt, maxTokens, temperature, preferredModel);
                if (StringUtils.isNotBlank(response)) {
                    return response;
                }
            } catch (Exception ex) {
                log.warn("[ai-gateway] provider={} scene={} 调用失败: {}", provider, scene, ex.getMessage());
            }
        }
        return "";
    }

    private List<String> buildProviderCandidates(String firstProvider, String fallbackProvider) {
        LinkedHashSet<String> providers = new LinkedHashSet<>();
        providers.add(normalizeProvider(firstProvider));
        providers.add(normalizeProvider(fallbackProvider));
        providers.add(currentProviderCode());
        if (properties.getFallbackProviders() != null) {
            for (String fallback : properties.getFallbackProviders()) {
                providers.add(normalizeProvider(fallback));
            }
        }
        providers.add("mock");
        providers.remove("");
        return new ArrayList<>(providers);
    }

    private String callProvider(ThirdPartyResolvedRoute route,
                                String scene,
                                String systemPrompt,
                                String userPrompt,
                                Integer maxTokens,
                                Double temperature,
                                String preferredModel) throws IOException, InterruptedException {
        ProviderConfig config = resolveProviderConfig(route, scene, preferredModel);
        return callProvider(config, systemPrompt, userPrompt, maxTokens, temperature);
    }

    private String callProvider(String provider,
                                String scene,
                                String systemPrompt,
                                String userPrompt,
                                Integer maxTokens,
                                Double temperature,
                                String preferredModel) throws IOException, InterruptedException {
        ProviderConfig config = resolveProviderConfig(provider, scene, preferredModel);
        return callProvider(config, systemPrompt, userPrompt, maxTokens, temperature);
    }

    private String callProvider(ProviderConfig config,
                                String systemPrompt,
                                String userPrompt,
                                Integer maxTokens,
                                Double temperature) throws IOException, InterruptedException {
        if (StringUtils.isBlank(config.apiKey)) {
            throw new IllegalStateException("未配置API Key");
        }

        Map<String, Object> requestBody = buildRequestBody(config, systemPrompt, userPrompt, maxTokens, temperature);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(config.requestUrl))
            .timeout(Duration.ofSeconds(Math.max(5, properties.getTimeoutSeconds())))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + config.apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("HTTP " + response.statusCode() + ": " + response.body());
        }

        return parseContent(response.body());
    }

    private String parseContent(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);

        JsonNode errorNode = root.path("error");
        if (!errorNode.isMissingNode() && !errorNode.isNull()) {
            String errMsg = errorNode.path("message").asText(body);
            throw new IllegalStateException(errMsg);
        }

        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            JsonNode message = choices.get(0).path("message");
            String content = extractText(message.path("content"));
            if (StringUtils.isNotBlank(content)) {
                return content.trim();
            }
        }

        String outputText = root.path("output_text").asText("");
        if (StringUtils.isNotBlank(outputText)) {
            return outputText.trim();
        }

        JsonNode output = root.path("output");
        if (output.isArray() && !output.isEmpty()) {
            for (JsonNode item : output) {
                if (item == null || item.isNull()) {
                    continue;
                }
                String content = extractText(item.path("content"));
                if (StringUtils.isNotBlank(content)) {
                    return content.trim();
                }
            }
        }
        return "";
    }

    private String extractText(JsonNode contentNode) {
        if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) {
            return "";
        }
        if (contentNode.isTextual()) {
            return contentNode.asText("");
        }
        if (contentNode.isArray()) {
            StringBuilder buffer = new StringBuilder();
            for (JsonNode item : contentNode) {
                if (item == null || item.isNull()) {
                    continue;
                }
                if (item.isTextual()) {
                    buffer.append(item.asText(""));
                    continue;
                }
                String text = item.path("text").asText("");
                if (StringUtils.isBlank(text)) {
                    text = item.path("content").asText("");
                }
                if (StringUtils.isNotBlank(text)) {
                    if (buffer.length() > 0) {
                        buffer.append('\n');
                    }
                    buffer.append(text);
                }
            }
            return buffer.toString();
        }
        return "";
    }

    private ProviderConfig resolveProviderConfig(ThirdPartyResolvedRoute route, String scene, String preferredModel) {
        String normalized = normalizeProvider(route.getProviderCode());
        LlmProviderStrategy strategy = resolveProviderStrategy(normalized);
        Map<String, String> configs = route.getConfigs() == null || route.getConfigs().isEmpty()
            ? loadProviderConfigs(normalized, strategy)
            : route.getConfigs();
        return buildProviderConfig(scene, strategy, configs, preferredModel);
    }

    private ProviderConfig resolveProviderConfig(String provider, String scene, String preferredModel) {
        String normalized = normalizeProvider(provider);
        LlmProviderStrategy strategy = resolveProviderStrategy(normalized);
        Map<String, String> configs = loadProviderConfigs(normalized, strategy);
        return buildProviderConfig(scene, strategy, configs, preferredModel);
    }

    private ProviderConfig buildProviderConfig(String scene,
                                               LlmProviderStrategy strategy,
                                               Map<String, String> configs,
                                               String preferredModel) {
        ProviderConfig providerConfig = new ProviderConfig();
        providerConfig.apiKey = strategy.resolveApiKey(configs, environment);
        providerConfig.model = StringUtils.defaultIfBlank(StringUtils.trimToEmpty(preferredModel), resolveModel(strategy, scene, configs));
        providerConfig.apiProtocol = resolveApiProtocol(configs);
        providerConfig.requestUrl = resolveRequestUrl(strategy, providerConfig.apiProtocol, configs);
        providerConfig.store = resolveStore(scene, configs);
        return providerConfig;
    }

    private Map<String, Object> buildRequestBody(ProviderConfig config,
                                                 String systemPrompt,
                                                 String userPrompt,
                                                 Integer maxTokens,
                                                 Double temperature) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", config.model);
        if ("responses".equals(config.apiProtocol)) {
            requestBody.put("input", List.of(
                Map.of(
                    "role", "system",
                    "content", List.of(Map.of("type", "input_text", "text", systemPrompt))
                ),
                Map.of(
                    "role", "user",
                    "content", List.of(Map.of("type", "input_text", "text", userPrompt))
                )
            ));
            requestBody.put("max_output_tokens", normalizeMaxTokens(maxTokens));
        } else {
            requestBody.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ));
            requestBody.put("max_tokens", normalizeMaxTokens(maxTokens));
        }
        requestBody.put("temperature", normalizeTemperature(temperature));
        if (config.store != null) {
            requestBody.put("store", config.store);
        }
        return requestBody;
    }

    private Map<String, String> buildRouteContext(String scene) {
        Map<String, String> context = new LinkedHashMap<>();
        String normalizedScene = normalizeScene(scene);
        context.put("scene_code", normalizedScene);
        context.put("function_type", normalizedScene);
        return context;
    }

    private Map<String, String> loadProviderConfigs(String normalizedProvider, LlmProviderStrategy strategy) {
        Map<String, String> configs = routeConfigService.getProviderConfigs("ai", normalizedProvider);
        if (configs != null && !configs.isEmpty()) {
            return configs;
        }
        if (strategy.aliases() != null) {
            for (String alias : strategy.aliases()) {
                String normalizedAlias = normalizeCode(alias);
                if (StringUtils.isBlank(normalizedAlias) || normalizedAlias.equals(normalizedProvider)) {
                    continue;
                }
                Map<String, String> aliasConfigs = routeConfigService.getProviderConfigs("ai", normalizedAlias);
                if (aliasConfigs != null && !aliasConfigs.isEmpty()) {
                    return aliasConfigs;
                }
            }
        }
        return configs == null ? new LinkedHashMap<>() : configs;
    }

    private String resolveModel(LlmProviderStrategy strategy, String scene, Map<String, String> configs) {
        String normalizedScene = normalizeScene(scene);
        String model = firstNonBlank(
            configs.get(normalizedScene + "_model"),
            configs.get("model_" + normalizedScene),
            configs.get("model"),
            configs.get("chat_model"),
            configs.get("chatModel")
        );
        if (StringUtils.isNotBlank(model)) {
            return model;
        }
        return StringUtils.defaultIfBlank(strategy.defaultModel(), "gpt-5-codex");
    }

    private String resolveRequestUrl(LlmProviderStrategy strategy, String apiProtocol, Map<String, String> configs) {
        String endpoint = firstNonBlank(
            configs.get("endpoint"),
            configs.get("baseURL"),
            configs.get("baseUrl"),
            configs.get("base_url"),
            configs.get("url")
        );
        if (StringUtils.isBlank(endpoint)) {
            endpoint = strategy.defaultEndpoint(environment);
        }
        if (StringUtils.isBlank(endpoint)) {
            endpoint = "https://api.openai.com/v1";
        }
        String normalized = StringUtils.removeEnd(endpoint.trim(), "/");
        if (normalized.endsWith("/chat/completions") || normalized.endsWith("/responses")) {
            return normalized;
        }
        if ("responses".equals(apiProtocol)) {
            return normalized + "/responses";
        }
        return normalized + "/chat/completions";
    }

    private String resolveApiProtocol(Map<String, String> configs) {
        String protocol = firstNonBlank(
            configs.get("api_protocol"),
            configs.get("apiProtocol"),
            configs.get("protocol"),
            configs.get("request_mode"),
            configs.get("requestMode")
        );
        String normalized = normalizeCode(protocol);
        if (StringUtils.isBlank(normalized)) {
            String endpoint = firstNonBlank(
                configs.get("endpoint"),
                configs.get("baseURL"),
                configs.get("baseUrl"),
                configs.get("base_url"),
                configs.get("url")
            );
            if (StringUtils.endsWithIgnoreCase(StringUtils.trimToEmpty(endpoint), "/responses")) {
                return "responses";
            }
            return "chat_completions";
        }
        if ("response".equals(normalized) || "responses_api".equals(normalized)) {
            return "responses";
        }
        return "responses".equals(normalized) ? "responses" : "chat_completions";
    }

    private Boolean resolveStore(String scene, Map<String, String> configs) {
        String normalizedScene = normalizeScene(scene);
        String raw = firstNonBlank(
            configs.get(normalizedScene + "_store"),
            configs.get("store_" + normalizedScene),
            configs.get("store"),
            configs.get("request_store"),
            configs.get("options.store")
        );
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        return parseBoolean(raw);
    }

    private Boolean parseBoolean(String value) {
        String normalized = StringUtils.lowerCase(StringUtils.trimToEmpty(value));
        return switch (normalized) {
            case "1", "true", "yes", "y", "on" -> Boolean.TRUE;
            case "0", "false", "no", "n", "off" -> Boolean.FALSE;
            default -> null;
        };
    }

    private String normalizeScene(String scene) {
        String normalized = normalizeCode(scene);
        if (StringUtils.isBlank(normalized)) {
            return "default";
        }
        return normalized;
    }

    private String normalizeProvider(String provider) {
        String normalized = normalizeCode(provider);
        if (StringUtils.isBlank(normalized)) {
            return "";
        }
        LlmProviderStrategy strategy = providerStrategyRegistry.get(normalized);
        if (strategy == null) {
            return normalized;
        }
        return normalizeCode(strategy.providerCode());
    }

    private LlmProviderStrategy resolveProviderStrategy(String normalizedProvider) {
        LlmProviderStrategy strategy = providerStrategyRegistry.get(normalizedProvider);
        if (strategy != null) {
            return strategy;
        }
        LlmProviderStrategy defaultStrategy = providerStrategyRegistry.get("openai_codex");
        if (defaultStrategy != null) {
            return defaultStrategy;
        }
        return FALLBACK_STRATEGY;
    }

    private void registerProviderStrategy(String provider, LlmProviderStrategy strategy) {
        String normalized = normalizeCode(provider);
        if (StringUtils.isBlank(normalized) || strategy == null) {
            return;
        }
        providerStrategyRegistry.put(normalized, strategy);
    }

    private String normalizeCode(String value) {
        return StringUtils.lowerCase(StringUtils.trimToEmpty(value)).replace('-', '_');
    }

    private Integer normalizeMaxTokens(Integer maxTokens) {
        if (maxTokens == null || maxTokens <= 0) {
            return 360;
        }
        return Math.min(1200, maxTokens);
    }

    private Double normalizeTemperature(Double temperature) {
        if (temperature == null) {
            return 0.7D;
        }
        double value = Math.max(0D, Math.min(1.5D, temperature));
        return value;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private static class FallbackStrategy implements LlmProviderStrategy {

        @Override
        public String providerCode() {
            return "openai_codex";
        }

        @Override
        public String resolveApiKey(Map<String, String> configs, Environment environment) {
            String fromConfig = StringUtils.trimToEmpty(
                StringUtils.defaultIfBlank(
                    configs.get("api_key"),
                    StringUtils.defaultIfBlank(
                        configs.get("apiKey"),
                        StringUtils.defaultIfBlank(configs.get("apikey"), configs.get("key"))
                    )
                )
            );
            if (StringUtils.isNotBlank(fromConfig)) {
                return fromConfig;
            }
            return StringUtils.defaultIfBlank(
                environment.getProperty("aigateway.openai.api-key"),
                environment.getProperty("OPENAI_API_KEY")
            );
        }

        @Override
        public String defaultModel() {
            return "gpt-5-codex";
        }

        @Override
        public String defaultEndpoint(Environment environment) {
            return "https://api.openai.com/v1";
        }
    }

    private static class ProviderConfig {
        private String apiKey;
        private String model;
        private String requestUrl;
        private String apiProtocol;
        private Boolean store;
    }
}
