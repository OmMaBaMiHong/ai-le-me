package org.aileme.shejiao.common.validator;

import org.aileme.shejiao.common.exception.LinfengException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;

/**
 * 校验工具类
 *
 */
public class ValidatorUtils {
    private static Validator validator;

    static {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    /**
     * 校验对象
     * @param object        待校验对象
     * @param groups        待校验的组
     * @throws LinfengException  校验不通过，则报LinfengException异常
     */
    public static void validateEntity(Object object, Class<?>... groups)
            throws LinfengException {
        Set<ConstraintViolation<Object>> constraintViolations = validator.validate(object, groups);
        if (!constraintViolations.isEmpty()) {
            StringBuilder msg = new StringBuilder();
            for(ConstraintViolation<Object> constraint:  constraintViolations){
                msg.append(constraint.getMessage()).append(" ");
            }
            throw new LinfengException(msg.toString());
        }
    }
}
