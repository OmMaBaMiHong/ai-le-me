package org.aileme.shejiao.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.aileme.shejiao.api.service.HongniangPrivateDomainService;
import org.aileme.shejiao.common.utils.R;

import java.util.Map;

@RestController
@RequestMapping("/admin/hongniang-private-domain")
@Tag(name = "管理端——红娘私域总览")
public class HongniangPrivateDomainDashboardController {

    private final HongniangPrivateDomainService privateDomainService;

    public HongniangPrivateDomainDashboardController(HongniangPrivateDomainService privateDomainService) {
        this.privateDomainService = privateDomainService;
    }

    @GetMapping("/overview")
    @Operation(summary = "红娘私域概览")
    public R overview(@RequestParam(required = false) Integer hongniangId) {
        return R.ok().put("data", privateDomainService.overview(hongniangId));
    }

    @GetMapping("/kanban")
    @Operation(summary = "红娘私域看板")
    public R kanban(@RequestParam Map<String, Object> params) {
        return R.ok().put("data", privateDomainService.kanban(params));
    }
}
