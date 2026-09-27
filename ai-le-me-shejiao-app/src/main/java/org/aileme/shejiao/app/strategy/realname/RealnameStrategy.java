package org.aileme.shejiao.app.strategy.realname;

import com.alibaba.fastjson.JSONObject;

/**
 * 实名认证策略接口
 *
 * 所有实名认证云服务商（阿里云、腾讯云等）都需要实现此接口
 */
public interface RealnameStrategy {

    /**
     * 身份二要素核验（姓名 + 身份证号）
     *
     * @param name   真实姓名
     * @param idCard 身份证号
     * @return 核验结果 JSON，至少包含以下字段：
     *         - "Result": "PASS" / "FAIL"（核验是否通过）
     *         - "Description": 描述信息
     *         - "Name": 脱敏后的姓名（可选）
     *         - "IdCard": 脱敏后的身份证号（可选）
     */
    JSONObject verify(String name, String idCard);

    /**
     * 获取策略名称（用于日志和调试）
     *
     * @return 策略名称，如 "aliyun" / "tencent"
     */
    String getProviderName();
}
