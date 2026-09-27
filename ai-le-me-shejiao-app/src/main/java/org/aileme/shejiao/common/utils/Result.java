package org.aileme.shejiao.common.utils;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

import java.io.Serializable;

/**
 * 公共返回对象  非hashmap方式
 * @author JL.Yu
 */
@Schema(title = "响应")
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 响应码：0表示成功，其他值表示失败
     */
    @Schema(title = "响应码:0表示成功,其他值表示失败")
    private int code = 0;
    /**
     * 消息内容
     */
    @Schema(title = "消息内容")
    private String msg = "success";
    /**
     * 响应数据
     */
    @Schema(title = "响应数据")
    private T result;

    public Result<T> ok(T result) {
        this.setResult(result);
        return this;
    }

    public Result<T> ok(String msg) {
        this.msg = msg;
        return this;
    }

    public Result<T> ok() {
        this.msg = "success";
        return this;
    }

    public Result(){
        this.msg = "success";
        this.code = 0;
    }

    public boolean success(){
        return code == 0;
    }

    public Result<T> error() {
        this.code = HttpStatus.INTERNAL_SERVER_ERROR.value();
        this.msg = "服务器出小差啦";
        return this;
    }

    public Result<T> error(int code) {
        this.code = code;
        this.msg = "服务器出小差啦";
        return this;
    }

    public Result<T> error(int code, String msg) {
        this.code = code;
        this.msg = msg;
        return this;
    }

    public Result<T> error(String msg) {
        this.code = HttpStatus.INTERNAL_SERVER_ERROR.value();
        this.msg = msg;
        return this;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }
}

