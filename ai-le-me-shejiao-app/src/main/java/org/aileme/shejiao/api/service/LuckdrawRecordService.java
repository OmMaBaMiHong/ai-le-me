package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.domain.vo.LuckdrawRecordResponse;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.LuckdrawRecordEntity;

import java.util.List;
import java.util.Map;

/**
 * 
 *
 * @author linfeng
 * @email 3582996245@qq.com
 * @date 2022-08-14 14:28:48
 */
public interface LuckdrawRecordService extends IService<LuckdrawRecordEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<LuckdrawRecordResponse> getLuckDrawRecordList();

    Integer getSurplus(AppUserEntity user);
}

