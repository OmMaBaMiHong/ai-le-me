package org.aileme.tools;

import org.aileme.AilemeApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

public class RobotSeedBatchRunner {

    public static void main(String[] args) throws Exception {
        System.setProperty("server.port", "0");
        System.setProperty("spring.boot.admin.client.enabled", "false");
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(AilemeApplication.class)
                .run(args)) {
            String runnerMode = System.getProperty("robot.seed.runnerMode", "existing_users");
            Object robotSeedService = context.getBean("robotSeedServiceImpl");
            Object result;
            if ("new_users".equalsIgnoreCase(runnerMode)) {
                Method method = robotSeedService.getClass().getMethod("generateBatchUsersWithPosts", int.class, int.class, int.class);
                result = method.invoke(
                        robotSeedService,
                        intProp("robot.seed.userCount", intProp("robot.seed.userLimit", 20)),
                        intProp("robot.seed.minPostsPerUser", 1),
                        intProp("robot.seed.maxPostsPerUser", 1)
                );
                System.out.println("ROBOT_SEED_NEW_USERS_RESULT=" + result);
            } else {
                Map<String, Object> options = new LinkedHashMap<>();
                options.put("userLimit", intProp("robot.seed.userLimit", 100));
                options.put("minPostsPerUser", intProp("robot.seed.minPostsPerUser", 1));
                options.put("maxPostsPerUser", intProp("robot.seed.maxPostsPerUser", 1));
                options.put("userScope", System.getProperty("robot.seed.userScope", "seed"));
                options.put("shuffle", boolProp("robot.seed.shuffle", true));
                options.put("createUserIfNeeded", false);
                options.put("contentFactoryMode", System.getProperty("robot.seed.contentFactoryMode", "hybrid"));
                options.put("preferredProvider", System.getProperty("robot.seed.preferredProvider", "doubao"));
                options.put("preferredModel", System.getProperty("robot.seed.preferredModel", ""));
                String uidList = System.getProperty("robot.seed.uidList", "");
                if (!uidList.isBlank()) {
                    options.put("uidList", uidList);
                }
                Method method = robotSeedService.getClass().getMethod("generatePostsForExistingUsers", Map.class);
                result = method.invoke(robotSeedService, options);
                System.out.println("ROBOT_SEED_BATCH_RESULT=" + result);
            }
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
