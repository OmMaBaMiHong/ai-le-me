/**
 * -----------------------------------
 * Copyright (c) 2021-2023
 * All rights reserved, Designed By my.hots.love
 *
 * 商业版授权联系技术客服	 QQ:  3582996245
 * 严禁分享、盗用、转卖源码或非法牟利！
 * 版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.app.controller;

import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.FileCheckUtil;
import org.aileme.shejiao.common.utils.R;
import org.aileme.system.service.ISysOssService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * APP文件上传
 *
 */
@RestController
@RequestMapping("/app/common")
@Tag(name = "移动端——文件上传")
public class AppOssController {

	@Value("${shejiao.oss.max-size:30}")
	private Long maxSize;

	@Autowired
	private ISysOssService sysOssService;


	@Operation(summary = "上传文件")
	@PostMapping("/upload")
	public R upload(@RequestParam("file") MultipartFile file) throws Exception {
		if (file.isEmpty()) {
			throw new LinfengException("上传文件不能为空");
		}
		FileCheckUtil.checkSize(maxSize, file.getSize());

		// 使用新系统的 OSS 服务上传文件
		var ossVo = sysOssService.upload(file);

		return R.ok().put("result", ossVo.getUrl());
	}



}
