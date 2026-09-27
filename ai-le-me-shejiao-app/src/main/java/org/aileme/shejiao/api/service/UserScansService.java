package org.aileme.shejiao.api.service;

import org.aileme.shejiao.domain.entity.app.UserScans;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 个人主页浏览记录 服务类
 * </p>
 *
 * @author lww
 * @since 2023-05-16
 */
public interface UserScansService extends IService<UserScans> {

    void updateScaNums(Integer id);
}
