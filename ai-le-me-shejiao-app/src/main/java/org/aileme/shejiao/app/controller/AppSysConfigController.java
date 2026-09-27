//package org.aileme.shejiao.app.controller;
//
//import cn.hutool.core.util.ObjectUtil;
//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.JSONObject;
//
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//import org.aileme.shejiao.api.service.SysConfigService;
//import org.aileme.shejiao.common.annotation.SysLog;
//import org.aileme.shejiao.common.utils.PageUtils;
//import org.aileme.shejiao.common.utils.R;
//import org.aileme.shejiao.common.validator.ValidatorUtils;
//import org.aileme.shejiao.domain.entity.sys.SysConfigEntity;
//
//
//import java.util.Map;
//
///**
// * 管理端系统配置信息
// *
// */
//@Tag(name = "管理端——系统配置信息")
//@RestController
//@RequestMapping("/admin/config")
//public class AppSysConfigController {
//
//	@Autowired
//	private SysConfigService sysConfigService;
//
//
//    @Operation(summary ="所有配置列表")
//	@GetMapping("/list")
//	public R list(@RequestParam Map<String, Object> params){
//		PageUtils page = sysConfigService.queryPage(params);
//
//		return R.ok().put("page", page);
//	}
//
//
//	@Operation(summary="根据ID配置信息")
//	@GetMapping("/info/{id}")
//	public R info(@PathVariable("id") Long id){
//		SysConfigEntity config = sysConfigService.getById(id);
//
//		return R.ok().put("config", config);
//	}
//
//
//	@Operation(summary="保存配置")
//	@SysLog("保存配置")
//	@PostMapping("/save")
//	public R save(@RequestBody SysConfigEntity config){
//		ValidatorUtils.validateEntity(config);
//
//		sysConfigService.saveConfig(config);
//
//		return R.ok();
//	}
//
//
//	@Operation(summary="修改配置")
//	@SysLog("修改配置")
//	@PostMapping("/update")
//	public R update(@RequestBody SysConfigEntity config){
//		ValidatorUtils.validateEntity(config);
//
//		sysConfigService.update(config);
//
//		return R.ok();
//	}
//
//
//	@Operation(summary="删除配置")
//	@SysLog("删除配置")
//	@PostMapping("/delete")
//	public R delete(@RequestBody Long[] ids){
//		sysConfigService.deleteBatch(ids);
//
//		return R.ok();
//	}
//
//
//	@Operation(summary="配置项的批量修改操作")
//	@SysLog("配置项的批量修改操作")
//	@PostMapping("/updateBatch")
//	public R updateBatch(@RequestBody String jsonStr){
//		JSONObject jsonObject = JSON.parseObject(jsonStr);
//		jsonObject.forEach(
//				(key,value)->{
//					SysConfigEntity sysConfig = sysConfigService.lambdaQuery().eq(SysConfigEntity::getParamKey, key).one();
//					SysConfigEntity sysConfigDto=new SysConfigEntity();
//					sysConfigDto.setParamKey(key);
//					sysConfigDto.setParamValue(value.toString());
//					if(ObjectUtil.isNull(sysConfig)){
//						sysConfigService.saveConfig(sysConfigDto);
//					}else{
//						sysConfigDto.setId(sysConfig.getId());
//						sysConfigService.update(sysConfigDto);
//					}
//				});
//
//		return R.ok();
//	}
//
//}
