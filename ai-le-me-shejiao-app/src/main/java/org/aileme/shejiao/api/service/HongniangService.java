package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.admin.HongniangInfoEntity;

import java.util.Map;

/**
 * 红娘Service
 *
 * @author system
 * @date 2026-01-27
 */
public interface HongniangService extends IService<HongniangInfoEntity> {

    /**
     * 分页查询红娘列表
     */
    PageUtils queryPage(Map<String, Object> params);

    /**
     * 保存红娘信息
     */
    void saveHongniang(HongniangInfoEntity hongniang);

    /**
     * 更新红娘信息
     */
    void updateHongniang(HongniangInfoEntity hongniang);

    /**
     * 根据手机号查询红娘
     */
    HongniangInfoEntity getByPhone(String phone);

    /**
     * 根据用户ID查询红娘
     */
    HongniangInfoEntity getByUserId(Integer userId);

    /**
     * 导入文件解析用户数据
     * @param file PDF或DOCX文件
     * @param hongniangId 红娘ID
     * @return 导入成功的用户数
     */
    int importUsersFromFile(MultipartFile file, Integer hongniangId) throws Exception;

    /**
     * 更新红娘统计数据
     */
    void updateStatistics(Integer hongniangId);

    /**
     * 获取红娘统计数据
     */
    Map<String, Object> getHongniangStats(Integer hongniangId);
    
    /**
     * 检查用户是否有权限访问指定租户
     */
    boolean canAccessTenant(Integer userId, Integer tenantId);
}
