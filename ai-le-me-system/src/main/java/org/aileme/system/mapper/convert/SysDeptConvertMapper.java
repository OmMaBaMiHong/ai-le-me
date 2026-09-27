package org.aileme.system.mapper.convert;

import org.mapstruct.Mapper;
import org.aileme.system.domain.bo.SysDeptBo;
import org.aileme.system.domain.SysDept;
import java.util.List;

/**
 * SysDeptBo 与 SysDept 的映射接口
 * 
 * @author MapStruct Generator
 */
@Mapper(componentModel = "spring")
public interface SysDeptConvertMapper {

    /**
     * Entity 转 VO
     */
    SysDeptBo toVo(SysDept entity);

    /**
     * VO 转 Entity
     */
    SysDept toEntity(SysDeptBo vo);

    /**
     * Entity 列表转 VO 列表
     */
    List<SysDeptBo> toVoList(List<SysDept> entityList);

    /**
     * VO 列表转 Entity 列表
     */
    List<SysDept> toEntityList(List<SysDeptBo> voList);
}
