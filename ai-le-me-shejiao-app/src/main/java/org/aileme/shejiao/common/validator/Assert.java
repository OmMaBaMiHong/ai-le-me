
package org.aileme.shejiao.common.validator;

import org.aileme.shejiao.common.exception.LinfengException;
import org.apache.commons.lang3.StringUtils;

/**
 * 数据校验
 *
 */
public abstract class Assert {

    public static void isBlank(String str, String message) {
        if (StringUtils.isBlank(str)) {
            throw new LinfengException(message);
        }
    }

    public static void isNull(Object object, String message) {
        if (object == null) {
            throw new LinfengException(message);
        }
    }
}
