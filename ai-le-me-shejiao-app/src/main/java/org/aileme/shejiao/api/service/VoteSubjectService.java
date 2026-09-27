package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.admin.VoteSubjectEntity;

import java.util.Map;

/**
 * 
 *
 * @author linfeng
 * @email linfengtech001@163.com
 * @date 2022-06-28 13:56:51
 */
public interface VoteSubjectService extends IService<VoteSubjectEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

