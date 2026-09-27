package org.aileme.system.mapper.convert;

import org.mapstruct.Mapper;
import org.aileme.system.domain.bo.SysUserBo;
import org.aileme.system.domain.SysUser;
import java.util.List;

/**
 * SysUserBo 与 SysUser 的映射接口
 * 
 * @author MapStruct Generator
 */
@Mapper(componentModel = "spring")
public interface SysUserConvertMapper {

    /**
     * Entity 转 VO
     */
    SysUserBo toVo(SysUser entity);

    /**
     * VO 转 Entity
     */
    SysUser toEntity(SysUserBo vo);

    /**
     * Entity 列表转 VO 列表
     */
    List<SysUserBo> toVoList(List<SysUser> entityList);

    /**
     * VO 列表转 Entity 列表
     */
    List<SysUser> toEntityList(List<SysUserBo> voList);
}
