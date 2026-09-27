package org.aileme.shejiao.app.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.AppTrackService;
import org.aileme.shejiao.app.dao.AppEventMetricDailyDao;
import org.aileme.shejiao.app.dao.AppEventTrackDao;
import org.aileme.shejiao.domain.entity.app.AppEventTrackEntity;
import org.aileme.shejiao.domain.param.app.TrackEventDTO;

import java.util.*;

/**
 * App 埋点服务实现
 */
@DS("master")
@Service("appTrackService")
public class AppTrackServiceImpl extends ServiceImpl<AppEventTrackDao, AppEventTrackEntity> implements AppTrackService {

    private static final int MAX_TEXT_LEN = 200;
    private static final int MAX_PROP_TEXT_LEN = 60;

    @Autowired
    private AppEventTrackDao appEventTrackDao;

    @Autowired
    private AppEventMetricDailyDao appEventMetricDailyDao;

    @Override
    public Map<String, Object> batchTrack(Integer currentUid, List<TrackEventDTO> events) {
        Map<String, Object> result = new HashMap<>();
        if (events == null || events.isEmpty()) {
            result.put("accepted", 0);
            result.put("duplicate", 0);
            result.put("dropped", 0);
            return result;
        }

        int accepted = 0;
        int duplicate = 0;
        int dropped = 0;

        for (TrackEventDTO dto : events) {
            if (dto == null || StringUtils.isBlank(dto.getEventId()) || StringUtils.isBlank(dto.getEventName())) {
                dropped++;
                continue;
            }

            AppEventTrackEntity entity = new AppEventTrackEntity();
            entity.setEventId(trim(dto.getEventId(), 64));
            entity.setEventName(trim(dto.getEventName(), 64));
            entity.setModule(trim(dto.getModule(), 64));
            entity.setPage(trim(dto.getPage(), 128));
            entity.setUid(dto.getUid() != null ? dto.getUid() : currentUid);
            entity.setTargetUid(dto.getTargetUid());
            entity.setBizId(trim(dto.getBizId(), 64));
            entity.setSourceType(trim(dto.getSourceType(), 64));
            entity.setClientTs(dto.getClientTs());
            entity.setTraceId(trim(dto.getTraceId(), 64));
            entity.setPropsJson(JSON.toJSONString(sanitizeProps(dto.getProps())));
            entity.setEventDate(resolveEventDate(dto.getClientTs()));

            int rows = appEventTrackDao.insertIgnore(entity);
            if (rows > 0) {
                accepted++;
                appEventMetricDailyDao.upsertCount(
                        entity.getEventDate(),
                        emptyToUnknown(entity.getEventName()),
                        emptyToUnknown(entity.getModule()),
                        emptyToUnknown(entity.getPage()),
                        1L
                );
            } else {
                duplicate++;
            }
        }

        result.put("accepted", accepted);
        result.put("duplicate", duplicate);
        result.put("dropped", dropped);
        result.put("total", events.size());
        return result;
    }

    @Override
    public List<Map<String, Object>> funnelProfileTag(Date from, Date to) {
        return appEventTrackDao.selectFunnelByDate(from, to);
    }

    @Override
    public List<String> collectBehaviorTags(Integer uid, int limit) {
        if (uid == null || uid <= 0) {
            return Collections.emptyList();
        }
        int safeLimit = Math.max(1, Math.min(30, limit));
        List<AppEventTrackEntity> list = appEventTrackDao.selectRecentByUid(uid, 240);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        LinkedHashSet<String> tags = new LinkedHashSet<>();
        for (AppEventTrackEntity entity : list) {
            if (tags.size() >= safeLimit) {
                break;
            }
            extractTag(tags, entity.getBizId());
            JSONObject props = safeParse(entity.getPropsJson());
            if (props == null) {
                continue;
            }
            extractTag(tags, props.getString("tag"));
            extractTag(tags, props.getString("tagName"));
            extractTag(tags, props.getString("topic"));
            extractTag(tags, props.getString("keyword"));
            extractTag(tags, props.getString("interest"));
            extractTag(tags, props.getString("hobby"));
            extractTag(tags, props.getString("city"));
        }
        return new ArrayList<>(tags);
    }

    private JSONObject safeParse(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return JSON.parseObject(json);
        } catch (Exception ex) {
            return null;
        }
    }

    private void extractTag(LinkedHashSet<String> tags, String value) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        String normalized = trim(value.replaceAll("\\s+", " "), MAX_PROP_TEXT_LEN);
        if (normalized.length() < 2) {
            return;
        }
        if (normalized.matches("^[0-9]+$")) {
            return;
        }
        tags.add(normalized);
    }

    private String trim(String value, int maxLen) {
        String v = StringUtils.trimToEmpty(value);
        if (v.length() > maxLen) {
            return v.substring(0, maxLen);
        }
        return v;
    }

    private String emptyToUnknown(String value) {
        return StringUtils.isBlank(value) ? "unknown" : value;
    }

    private Date resolveEventDate(Long clientTs) {
        if (clientTs == null || clientTs <= 0) {
            return dateOnly(new Date());
        }
        return dateOnly(new Date(clientTs));
    }

    private Date dateOnly(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Map<String, Object> sanitizeProps(Map<String, Object> props) {
        if (props == null || props.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            if (entry == null || StringUtils.isBlank(entry.getKey()) || result.size() >= 20) {
                continue;
            }
            String key = trim(entry.getKey(), 40);
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            if (value instanceof Number || value instanceof Boolean) {
                result.put(key, value);
            } else {
                result.put(key, trim(String.valueOf(value), MAX_TEXT_LEN));
            }
        }
        return result;
    }
}
