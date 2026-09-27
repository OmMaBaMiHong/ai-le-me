package org.aileme.shejiao.common.utils;

/**
 * Redis所有系统配置的Key添加前缀
 *
 */
public class RedisKeys {
    public static String WX_LOGIN_CODE_="wx_login_code_";

    public static String getSysConfigKey(String key){
        return "sys:config:" + key;
    }

    public static String getPostKey(long postId){
        return "post:info:" + postId;
    }

    /**
     * 粉丝关注量缓存
     * @param uid 用户ID
     * @return 缓存key
     */
    public static String getUserKey(Integer uid){
        return "user:info:" + uid;
    }

    /**
     * 用户登录信息缓存
     * @param uid 用户ID
     * @return 缓存key
     */
    public static String getUserCacheKey(Integer uid){
        return "userId:" + uid;
    }
}
