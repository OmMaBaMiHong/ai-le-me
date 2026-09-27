package org.aileme.system.mapper.convert;

import org.mapstruct.Mapper;
import org.aileme.system.domain.bo.SysOperLogBo;
import org.aileme.system.domain.SysOperLog;
import java.util.List;

/**
 * SysOperLogBo 与 SysOperLog 的映射接口
 * 
 * @author MapStruct Generator
 */
@Mapper(componentModel = "spring")
public interface SysOperLogConvertMapper {

    /**
     * Entity 转 VO
     */
    SysOperLogBo toVo(SysOperLog entity);

    /**
     * VO 转 Entity
     */
    SysOperLog toEntity(SysOperLogBo vo);

    /**
     * Entity 列表转 VO 列表
     */
    List<SysOperLogBo> toVoList(List<SysOperLog> entityList);

    /**
     * VO 列表转 Entity 列表
     */
    List<SysOperLog> toEntityList(List<SysOperLogBo> voList);
}
