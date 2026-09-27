package org.aileme.shejiao.api.service;

/**
 * 短信发送统一入口。
 *
 * <p>运行时只读取 sys_third_party_provider.config_json。
 */
public interface SmsSenderService {

    void sendLoginCode(String mobile, String code);
}
