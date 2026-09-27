package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 * 手机号授权
 * @author linfeng
 * @date 2022/11/1 13:42
 */
//@Data  // Temporarily commented out due to Maven compilation issue
@Schema(title = "LoginPhoneParam", description = "手机号授权")
public class LoginPhoneParam {

    /**
     * 微信openID
     */
    @Schema(title = "微信openID")
    private String wechatOpenId;

    /**
     * 加密秘钥
     */
    @Schema(title = "加密秘钥")
    private String sessionKey;

    /**
     * 加密数据
     */
    @Schema(title = "加密数据")
    private String encryptedData;

    /**
     * 加密算法初始向量
     */
    @Schema(title = "加密算法初始向量")
    private String iv;

    // Added missing getter/setter methods for compilation
    public String getWechatOpenId() {
        return wechatOpenId;
    }

    public void setWechatOpenId(String wechatOpenId) {
        this.wechatOpenId = wechatOpenId;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public String getEncryptedData() {
        return encryptedData;
    }

    public void setEncryptedData(String encryptedData) {
        this.encryptedData = encryptedData;
    }

    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }
}