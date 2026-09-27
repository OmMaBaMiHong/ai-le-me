package org.aileme.shejiao.common.exception;

/**
 * 小程序备案能力关闭时使用成功空响应收口，避免前端收到异常态。
 */
public class MiniAppFeatureDisabledException extends LinfengException {

    private static final long serialVersionUID = 1L;

    public MiniAppFeatureDisabledException(String msg) {
        super(msg);
    }
}
