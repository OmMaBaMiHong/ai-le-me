package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.app.VideoTemplateEntity;

import java.util.Map;

/**
 * 视频模板服务接口
 *
 * @author system
 * @date 2026-02-13
 */
public interface VideoTemplateService extends IService<VideoTemplateEntity> {

    /**
     * 根据编码获取模板
     *
     * @param code 模板编码
     * @return 模板实体
     */
    VideoTemplateEntity getByCode(String code);

    /**
     * 分页查询模板列表（管理端）
     *
     * @param params 查询参数
     * @return 分页结果
     */
    PageUtils queryPage(Map<String, Object> params);
}
