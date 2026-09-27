package org.aileme.common.core.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MapStruct 工具类
 * <p>迁移说明：已从 MapStruct-Plus 迁移到原生 MapStruct 1.6.3</p>
 * <p>当前实现使用 Spring BeanUtils 作为 fallback，性能略低于直接使用 Mapper</p>
 * <p>建议：对于性能敏感的场景，直接注入对应的 Mapper 使用</p>
 *
 * @author Michelle.Chung
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MapstructUtils {

    /**
     * 将 T 类型对象，转换为 desc 类型的对象并返回
     * <p>当前实现：使用 BeanUtils.copyProperties 作为 fallback</p>
     *
     * @param source 数据来源实体
     * @param desc   描述对象 转换后的对象
     * @return desc
     */
    public static <T, V> V convert(T source, Class<V> desc) {
        if (ObjectUtil.isNull(source)) {
            return null;
        }
        if (ObjectUtil.isNull(desc)) {
            return null;
        }
        try {
            V target = desc.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            log.error("对象转换失败: {} -> {}", source.getClass().getName(), desc.getName(), e);
            return null;
        }
    }

    /**
     * 将 T 类型的集合，转换为 desc 类型的集合并返回
     * <p>当前实现：使用 BeanUtils.copyProperties 作为 fallback</p>
     *
     * @param sourceList 数据来源实体列表
     * @param desc       描述对象 转换后的对象
     * @return desc
     */
    public static <T, V> List<V> convert(List<T> sourceList, Class<V> desc) {
        if (ObjectUtil.isNull(sourceList)) {
            return null;
        }
        if (CollUtil.isEmpty(sourceList)) {
            return CollUtil.newArrayList();
        }
        return sourceList.stream()
            .map(source -> convert(source, desc))
            .collect(Collectors.toList());
    }

}

