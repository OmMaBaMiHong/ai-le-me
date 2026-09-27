package org.aileme.system.mapper.convert;

import org.mapstruct.Mapper;
import org.aileme.system.domain.bo.SysOssBo;
import org.aileme.system.domain.SysOss;
import java.util.List;

/**
 * SysOssBo 与 SysOss 的映射接口
 * 
 * @author MapStruct Generator
 */
@Mapper(componentModel = "spring")
public interface SysOssConvertMapper {

    /**
     * Entity 转 VO
     */
    SysOssBo toVo(SysOss entity);

    /**
     * VO 转 Entity
     */
    SysOss toEntity(SysOssBo vo);

    /**
     * Entity 列表转 VO 列表
     */
    List<SysOssBo> toVoList(List<SysOss> entityList);

    /**
     * VO 列表转 Entity 列表
     */
    List<SysOss> toEntityList(List<SysOssBo> voList);
}
