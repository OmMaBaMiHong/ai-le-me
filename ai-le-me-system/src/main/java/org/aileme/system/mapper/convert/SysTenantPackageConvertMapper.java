package org.aileme.system.mapper.convert;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.aileme.system.domain.bo.SysTenantPackageBo;
import org.aileme.system.domain.SysTenantPackage;
import java.util.List;

/**
 * SysTenantPackageBo 与 SysTenantPackage 的映射接口
 * 
 * @author MapStruct Generator
 */
@Mapper(componentModel = "spring")
public interface SysTenantPackageConvertMapper {

    /**
     * Entity 转 VO
     * menuIds 字段需要特殊处理：String -> Long[]
     */
    @Mapping(target = "menuIds", expression = "java(convertMenuIds(entity.getMenuIds()))")
    SysTenantPackageBo toVo(SysTenantPackage entity);

    /**
     * VO 转 Entity
     * menuIds 字段需要特殊处理：Long[] -> String
     */
    @Mapping(target = "menuIds", expression = "java(joinMenuIds(vo.getMenuIds()))")
    SysTenantPackage toEntity(SysTenantPackageBo vo);

    /**
     * Entity 列表转 VO 列表
     */
    List<SysTenantPackageBo> toVoList(List<SysTenantPackage> entityList);

    /**
     * VO 列表转 Entity 列表
     */
    List<SysTenantPackage> toEntityList(List<SysTenantPackageBo> voList);

    /**
     * String 转 Long[]
     */
    default Long[] convertMenuIds(String menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return new Long[0];
        }
        String[] parts = menuIds.split(",");
        Long[] result = new Long[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Long.parseLong(parts[i].trim());
        }
        return result;
    }

    /**
     * Long[] 转 String
     */
    default String joinMenuIds(Long[] menuIds) {
        if (menuIds == null || menuIds.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < menuIds.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(menuIds[i]);
        }
        return sb.toString();
    }
}
