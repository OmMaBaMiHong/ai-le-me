package org.aileme.shejiao.api.service;

import java.util.Map;

public interface RobotSeedService {

    Map<String, Object> generateBatchUsersWithPosts(int userCount, int minPostsPerUser, int maxPostsPerUser);

    Map<String, Object> generatePostsForExistingUsers(Map<String, Object> options);

    Map<String, Object> refreshExistingUserFigures(Map<String, Object> options);

    Map<String, Object> generateSinglePost(boolean createUserIfNeeded);

    Map<String, Object> generateSinglePostWithOptions(Map<String, Object> options);

    Map<String, Object> ensureDefaultSinglePostQuartzJob();

    Map<String, Object> ensureDefaultAiFactoryQuartzJob();
}
