package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.admin.SensitiveEntity;

import java.util.Map;

/**
 * 敏感词库信息表
 *
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-28 13:40:57
 */
public interface SensitiveService extends IService<SensitiveEntity> {

    PageUtils queryPage(Map<String, Object> params);

    Boolean checkContent(String content);

    void checkPostContent(String content);
}

