package org.aileme.system.mapper.convert;

import org.mapstruct.Mapper;
import org.aileme.system.domain.bo.SysClientBo;
import org.aileme.system.domain.SysClient;
import java.util.List;

/**
 * SysClientBo 与 SysClient 的映射接口
 * 
 * @author MapStruct Generator
 */
@Mapper(componentModel = "spring")
public interface SysClientConvertMapper {

    /**
     * Entity 转 VO
     */
    SysClientBo toVo(SysClient entity);

    /**
     * VO 转 Entity
     */
    SysClient toEntity(SysClientBo vo);

    /**
     * Entity 列表转 VO 列表
     */
    List<SysClientBo> toVoList(List<SysClient> entityList);

    /**
     * VO 列表转 Entity 列表
     */
    List<SysClient> toEntityList(List<SysClientBo> voList);
}
