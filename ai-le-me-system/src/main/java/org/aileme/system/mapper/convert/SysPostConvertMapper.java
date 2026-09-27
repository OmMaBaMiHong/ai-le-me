package org.aileme.system.mapper.convert;

import org.mapstruct.Mapper;
import org.aileme.system.domain.bo.SysPostBo;
import org.aileme.system.domain.SysPost;
import java.util.List;

/**
 * SysPostBo 与 SysPost 的映射接口
 * 
 * @author MapStruct Generator
 */
@Mapper(componentModel = "spring")
public interface SysPostConvertMapper {

    /**
     * Entity 转 VO
     */
    SysPostBo toVo(SysPost entity);

    /**
     * VO 转 Entity
     */
    SysPost toEntity(SysPostBo vo);

    /**
     * Entity 列表转 VO 列表
     */
    List<SysPostBo> toVoList(List<SysPost> entityList);

    /**
     * VO 列表转 Entity 列表
     */
    List<SysPost> toEntityList(List<SysPostBo> voList);
}
