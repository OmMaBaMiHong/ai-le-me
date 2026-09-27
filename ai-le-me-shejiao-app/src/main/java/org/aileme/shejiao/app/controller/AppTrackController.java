package org.aileme.shejiao.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.aileme.shejiao.api.service.AppTrackService;
import org.aileme.shejiao.api.service.SysConfigService;
import org.aileme.shejiao.app.annotation.Login;
import org.aileme.shejiao.app.annotation.LoginUser;
import org.aileme.shejiao.common.utils.Result;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.param.app.TrackEventDTO;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * App 埋点接口
 */
@RestController
@RequestMapping("/app/track")
@Tag(name = "移动端——行为埋点")
public class AppTrackController {

    @Autowired
    private AppTrackService appTrackService;

    @Autowired
    private SysConfigService sysConfigService;

    @Login
    @PostMapping("/batch")
    @Operation(summary = "批量上报埋点")
    public Result<Map<String, Object>> batchTrack(
            @Parameter(hidden = true) @LoginUser AppUserEntity user,
            @RequestBody(required = false) List<TrackEventDTO> events
    ) {
        if (!isEnabled("track_enabled", true)) {
            Map<String, Object> disabled = new HashMap<>();
            disabled.put("accepted", 0);
            disabled.put("duplicate", 0);
            disabled.put("dropped", events == null ? 0 : events.size());
            disabled.put("disabled", true);
            return new Result<Map<String, Object>>().ok(disabled);
        }

        Map<String, Object> result = appTrackService.batchTrack(user.getUid(), events);
        result.put("disabled", false);
        return new Result<Map<String, Object>>().ok(result);
    }

    @Login
    @GetMapping("/funnel/profile-tag")
    @Operation(summary = "画像标签漏斗统计")
    public Result<Map<String, Object>> profileTagFunnel(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        LocalDate toDate = parseDateOrDefault(to, LocalDate.now());
        LocalDate fromDate = parseDateOrDefault(from, toDate.minusDays(6));
        if (fromDate.isAfter(toDate)) {
            LocalDate tmp = fromDate;
            fromDate = toDate;
            toDate = tmp;
        }

        Date fromDateObj = Date.from(fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date toDateObj = Date.from(toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusSeconds(1).toInstant());

        List<Map<String, Object>> stats = appTrackService.funnelProfileTag(fromDateObj, toDateObj);
        Map<String, Object> result = new HashMap<>();
        result.put("from", fromDate.toString());
        result.put("to", toDate.toString());
        result.put("items", stats);
        return new Result<Map<String, Object>>().ok(result);
    }

    private LocalDate parseDateOrDefault(String value, LocalDate fallback) {
        if (StringUtils.isBlank(value)) {
            return fallback;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception ex) {
            return fallback;
        }
    }

    private boolean isEnabled(String key, boolean defaultValue) {
        try {
            String value = sysConfigService.getValue(key);
            if (StringUtils.isBlank(value)) {
                return defaultValue;
            }
            return !"0".equals(value.trim());
        } catch (Exception ex) {
            return defaultValue;
        }
    }
}
