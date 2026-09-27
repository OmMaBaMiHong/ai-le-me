/**
 * -----------------------------------
 *  Copyright (c) 2021-2023
 *  All rights reserved, Designed By my.hots.love
 *
 *  商业版授权联系技术客服	 QQ:  3582996245
 *  严禁分享、盗用、转卖源码或非法牟利！
 *  版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.common.exception;

import org.aileme.shejiao.common.utils.R;
// /* import org.apache.shiro.authz.AuthorizationException; */ // Temporarily removed due to Spring Boot 3.x compatibility // Removed due to Spring Boot 3.x compatibility issues
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;

/**
 * 异常处理器
 *
 */
@RestControllerAdvice
public class LinfengExceptionHandler {
	private Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * 处理小程序备案能力关闭
	 */
	@ExceptionHandler(MiniAppFeatureDisabledException.class)
	public R handleMiniAppFeatureDisabled(MiniAppFeatureDisabledException e){
		return R.ok()
			.put("msg", "success")
			.put("result", null)
			.put("data", null)
			.put("list", List.of());
	}

	/**
	 * 处理自定义异常
	 */
	@ExceptionHandler(LinfengException.class)
	public R handleException(LinfengException e){
		R r = new R();
		r.put("code", e.getCode());
		r.put("msg", e.getMessage());

		return r;
	}

	@ExceptionHandler(NoHandlerFoundException.class)
	public R handlerNoFoundException(Exception e) {
		logger.error(e.getMessage(), e);
		return R.error(404, "路径不存在，请检查路径是否正确");
	}

	@ExceptionHandler(DuplicateKeyException.class)
	public R handleDuplicateKeyException(DuplicateKeyException e){
		logger.error(e.getMessage(), e);
		return R.error("数据库中已存在该记录");
	}

	// Temporarily commented out due to Shiro removal
	/*
	@ExceptionHandler(AuthorizationException.class)
	public R handleAuthorizationException(AuthorizationException e){
		logger.error(e.getMessage(), e);
		return R.error("暂无权限请联系管理员");
	}
	*/

	@ExceptionHandler(Exception.class)
	public R handleException(Exception e){
		logger.error(e.getMessage(), e);
		return R.error();
	}
}
