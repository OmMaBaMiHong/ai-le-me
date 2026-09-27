package org.aileme.system.mapper.convert;

import org.mapstruct.Mapper;
import org.aileme.system.domain.bo.SysMenuBo;
import org.aileme.system.domain.SysMenu;
import java.util.List;

/**
 * SysMenuBo 与 SysMenu 的映射接口
 * 
 * @author MapStruct Generator
 */
@Mapper(componentModel = "spring")
public interface SysMenuConvertMapper {

    /**
     * Entity 转 VO
     */
    SysMenuBo toVo(SysMenu entity);

    /**
     * VO 转 Entity
     */
    SysMenu toEntity(SysMenuBo vo);

    /**
     * Entity 列表转 VO 列表
     */
    List<SysMenuBo> toVoList(List<SysMenu> entityList);

    /**
     * VO 列表转 Entity 列表
     */
    List<SysMenu> toEntityList(List<SysMenuBo> voList);
}
