package org.aileme.system.mapper.convert;

import org.mapstruct.Mapper;
import org.aileme.system.domain.bo.SysSocialBo;
import org.aileme.system.domain.SysSocial;
import java.util.List;

/**
 * SysSocialBo 与 SysSocial 的映射接口
 * 
 * @author MapStruct Generator
 */
@Mapper(componentModel = "spring")
public interface SysSocialConvertMapper {

    /**
     * Entity 转 VO
     */
    SysSocialBo toVo(SysSocial entity);

    /**
     * VO 转 Entity
     */
    SysSocial toEntity(SysSocialBo vo);

    /**
     * Entity 列表转 VO 列表
     */
    List<SysSocialBo> toVoList(List<SysSocial> entityList);

    /**
     * VO 列表转 Entity 列表
     */
    List<SysSocial> toEntityList(List<SysSocialBo> voList);
}
