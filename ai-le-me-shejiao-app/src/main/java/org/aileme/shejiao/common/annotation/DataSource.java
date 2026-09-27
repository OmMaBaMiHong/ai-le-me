package org.aileme.shejiao.common.annotation;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.aileme.shejiao.common.enums.DataSourceType;

import java.lang.annotation.*;

/**
 * 数据源切换注解
 *
 * @author hongniang
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataSource {
    DataSourceType value();
}
