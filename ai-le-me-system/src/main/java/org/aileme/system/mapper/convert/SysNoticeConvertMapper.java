package org.aileme.system.mapper.convert;

import org.mapstruct.Mapper;
import org.aileme.system.domain.bo.SysNoticeBo;
import org.aileme.system.domain.SysNotice;
import java.util.List;

/**
 * SysNoticeBo 与 SysNotice 的映射接口
 * 
 * @author MapStruct Generator
 */
@Mapper(componentModel = "spring")
public interface SysNoticeConvertMapper {

    /**
     * Entity 转 VO
     */
    SysNoticeBo toVo(SysNotice entity);

    /**
     * VO 转 Entity
     */
    SysNotice toEntity(SysNoticeBo vo);

    /**
     * Entity 列表转 VO 列表
     */
    List<SysNoticeBo> toVoList(List<SysNotice> entityList);

    /**
     * VO 列表转 Entity 列表
     */
    List<SysNotice> toEntityList(List<SysNoticeBo> voList);
}
