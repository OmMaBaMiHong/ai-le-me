package org.aileme.system.mapper.convert;

import org.mapstruct.Mapper;
import org.aileme.system.domain.bo.SysTenantBo;
import org.aileme.system.domain.SysTenant;
import java.util.List;

/**
 * SysTenantBo 与 SysTenant 的映射接口
 * 
 * @author MapStruct Generator
 */
@Mapper(componentModel = "spring")
public interface SysTenantConvertMapper {

    /**
     * Entity 转 VO
     */
    SysTenantBo toVo(SysTenant entity);

    /**
     * VO 转 Entity
     */
    SysTenant toEntity(SysTenantBo vo);

    /**
     * Entity 列表转 VO 列表
     */
    List<SysTenantBo> toVoList(List<SysTenant> entityList);

    /**
     * VO 列表转 Entity 列表
     */
    List<SysTenant> toEntityList(List<SysTenantBo> voList);
}
