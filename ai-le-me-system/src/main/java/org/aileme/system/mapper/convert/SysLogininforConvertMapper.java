package org.aileme.system.mapper.convert;

import org.mapstruct.Mapper;
import org.aileme.system.domain.bo.SysLogininforBo;
import org.aileme.system.domain.SysLogininfor;
import java.util.List;

/**
 * SysLogininforBo 与 SysLogininfor 的映射接口
 * 
 * @author MapStruct Generator
 */
@Mapper(componentModel = "spring")
public interface SysLogininforConvertMapper {

    /**
     * Entity 转 VO
     */
    SysLogininforBo toVo(SysLogininfor entity);

    /**
     * VO 转 Entity
     */
    SysLogininfor toEntity(SysLogininforBo vo);

    /**
     * Entity 列表转 VO 列表
     */
    List<SysLogininforBo> toVoList(List<SysLogininfor> entityList);

    /**
     * VO 列表转 Entity 列表
     */
    List<SysLogininfor> toEntityList(List<SysLogininforBo> voList);
}
