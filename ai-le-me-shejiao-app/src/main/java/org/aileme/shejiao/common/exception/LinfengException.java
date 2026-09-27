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

/**
 * 自定义异常
 *
 */
public class LinfengException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
    private String msg;
    private int code = 500;
    
    public LinfengException(String msg) {
		super(msg);
		this.msg = msg;
	}
	
	public LinfengException(String msg, Throwable e) {
		super(msg, e);
		this.msg = msg;
	}
	
	public LinfengException(String msg, int code) {
		super(msg);
		this.msg = msg;
		this.code = code;
	}
	
	public LinfengException(String msg, int code, Throwable e) {
		super(msg, e);
		this.msg = msg;
		this.code = code;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}
	
	
}
