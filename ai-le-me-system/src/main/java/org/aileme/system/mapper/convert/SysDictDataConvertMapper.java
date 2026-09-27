package org.aileme.system.mapper.convert;

import org.mapstruct.Mapper;
import org.aileme.system.domain.bo.SysDictDataBo;
import org.aileme.system.domain.SysDictData;
import java.util.List;

/**
 * SysDictDataBo 与 SysDictData 的映射接口
 * 
 * @author MapStruct Generator
 */
@Mapper(componentModel = "spring")
public interface SysDictDataConvertMapper {

    /**
     * Entity 转 VO
     */
    SysDictDataBo toVo(SysDictData entity);

    /**
     * VO 转 Entity
     */
    SysDictData toEntity(SysDictDataBo vo);

    /**
     * Entity 列表转 VO 列表
     */
    List<SysDictDataBo> toVoList(List<SysDictData> entityList);

    /**
     * VO 列表转 Entity 列表
     */
    List<SysDictData> toEntityList(List<SysDictDataBo> voList);
}
