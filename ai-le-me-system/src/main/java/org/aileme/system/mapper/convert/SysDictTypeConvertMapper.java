package org.aileme.system.mapper.convert;

import org.mapstruct.Mapper;
import org.aileme.system.domain.bo.SysDictTypeBo;
import org.aileme.system.domain.SysDictType;
import java.util.List;

/**
 * SysDictTypeBo 与 SysDictType 的映射接口
 * 
 * @author MapStruct Generator
 */
@Mapper(componentModel = "spring")
public interface SysDictTypeConvertMapper {

    /**
     * Entity 转 VO
     */
    SysDictTypeBo toVo(SysDictType entity);

    /**
     * VO 转 Entity
     */
    SysDictType toEntity(SysDictTypeBo vo);

    /**
     * Entity 列表转 VO 列表
     */
    List<SysDictTypeBo> toVoList(List<SysDictType> entityList);

    /**
     * VO 列表转 Entity 列表
     */
    List<SysDictType> toEntityList(List<SysDictTypeBo> voList);
}
