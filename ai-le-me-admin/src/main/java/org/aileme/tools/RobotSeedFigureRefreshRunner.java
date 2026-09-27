package org.aileme.tools;

import org.aileme.AilemeApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

public class RobotSeedFigureRefreshRunner {

    public static void main(String[] args) throws Exception {
        System.setProperty("server.port", "0");
        System.setProperty("spring.boot.admin.client.enabled", "false");
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(AilemeApplication.class)
                .run(args)) {
            Map<String, Object> options = new LinkedHashMap<>();
            options.put("userLimit", intProp("robot.seed.userLimit", 120));
            options.put("userScope", System.getProperty("robot.seed.userScope", "seed"));
            options.put("shuffle", boolProp("robot.seed.shuffle", true));
            options.put("onlyDirty", boolProp("robot.seed.onlyDirty", true));
            options.put("minImages", intProp("robot.seed.minImages", 1));
            options.put("maxImages", intProp("robot.seed.maxImages", 3));
            options.put("createUserIfNeeded", false);
            options.put("contentFactoryMode", System.getProperty("robot.seed.contentFactoryMode", "ai"));
            options.put("preferredProvider", System.getProperty("robot.seed.preferredProvider", "doubao"));
            options.put("preferredModel", System.getProperty("robot.seed.preferredModel", ""));

            Object robotSeedService = context.getBean("robotSeedServiceImpl");
            Method method = robotSeedService.getClass().getMethod("refreshExistingUserFigures", Map.class);
            Object result = method.invoke(robotSeedService, options);
            System.out.println("ROBOT_SEED_FIGURE_REFRESH_RESULT=" + result);
        }
    }

    private static int intProp(String key, int defaultValue) {
        try {
            return Integer.parseInt(System.getProperty(key, String.valueOf(defaultValue)));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static boolean boolProp(String key, boolean defaultValue) {
        return Boolean.parseBoolean(System.getProperty(key, String.valueOf(defaultValue)));
    }
}
