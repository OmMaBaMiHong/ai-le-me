package org.aileme.shejiao.admin.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import org.aileme.shejiao.api.service.UserImpressionTagService;
import org.aileme.shejiao.app.dao.UserImpressionTagDao;
import org.aileme.shejiao.domain.entity.app.UserImpressionTagEntity;

import java.util.*;

/**
 * 用户印象标签 Service 实现
 */
@DS("master")
@Service("userImpressionTagService")
public class UserImpressionTagServiceImpl extends ServiceImpl<UserImpressionTagDao, UserImpressionTagEntity>
    implements UserImpressionTagService {

    @Override
    @DSTransactional
    public void saveUserImpressions(Integer fromUserId, Integer toUserId, List<Integer> tagIds, String sourceType, Integer sourceId) {
        if (fromUserId == null || toUserId == null) {
            return;
        }
        // 删除旧的印象记录
        this.remove(new QueryWrapper<UserImpressionTagEntity>()
            .eq("from_user_id", fromUserId)
            .eq("to_user_id", toUserId));

        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }

        Date now = new Date();
        List<UserImpressionTagEntity> list = new ArrayList<>();
        for (Integer tagId : tagIds) {
            UserImpressionTagEntity entity = new UserImpressionTagEntity();
            entity.setFromUserId(fromUserId);
            entity.setToUserId(toUserId);
            entity.setTagId(tagId);
            entity.setSourceType(sourceType);
            entity.setSourceId(sourceId);
            entity.setCreateTime(now);
            list.add(entity);
        }
        this.saveBatch(list);
    }

    @Override
    public Map<Integer, Integer> getImpressionSummary(Integer toUserId) {
        if (toUserId == null) {
            return Collections.emptyMap();
        }
        QueryWrapper<UserImpressionTagEntity> wrapper = new QueryWrapper<>();
        wrapper.select("tag_id", "COUNT(*) AS cnt")
            .eq("to_user_id", toUserId)
            .groupBy("tag_id");

        List<Map<String, Object>> maps = this.listMaps(wrapper);
        Map<Integer, Integer> result = new HashMap<>();
        for (Map<String, Object> map : maps) {
            Object tagIdObj = map.get("tag_id");
            Object cntObj = map.get("cnt");
            if (tagIdObj != null && cntObj != null) {
                Integer tagId = ((Number) tagIdObj).intValue();
                Integer cnt = ((Number) cntObj).intValue();
                result.put(tagId, cnt);
            }
        }
        return result;
    }
}
