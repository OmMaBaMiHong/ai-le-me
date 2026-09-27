package org.aileme.system.mapper.convert;

import org.mapstruct.Mapper;
import org.aileme.system.domain.bo.SysConfigBo;
import org.aileme.system.domain.SysConfig;
import java.util.List;

/**
 * SysConfigBo 与 SysConfig 的映射接口
 * 
 * @author MapStruct Generator
 */
@Mapper(componentModel = "spring")
public interface SysConfigConvertMapper {

    /**
     * Entity 转 VO
     */
    SysConfigBo toVo(SysConfig entity);

    /**
     * VO 转 Entity
     */
    SysConfig toEntity(SysConfigBo vo);

    /**
     * Entity 列表转 VO 列表
     */
    List<SysConfigBo> toVoList(List<SysConfig> entityList);

    /**
     * VO 列表转 Entity 列表
     */
    List<SysConfig> toEntityList(List<SysConfigBo> voList);
}
