package org.aileme.system.mapper.convert;

import org.mapstruct.Mapper;
import org.aileme.system.domain.bo.SysRoleBo;
import org.aileme.system.domain.SysRole;
import java.util.List;

/**
 * SysRoleBo 与 SysRole 的映射接口
 * 
 * @author MapStruct Generator
 */
@Mapper(componentModel = "spring")
public interface SysRoleConvertMapper {

    /**
     * Entity 转 VO
     */
    SysRoleBo toVo(SysRole entity);

    /**
     * VO 转 Entity
     */
    SysRole toEntity(SysRoleBo vo);

    /**
     * Entity 列表转 VO 列表
     */
    List<SysRoleBo> toVoList(List<SysRole> entityList);

    /**
     * VO 列表转 Entity 列表
     */
    List<SysRole> toEntityList(List<SysRoleBo> voList);
}
