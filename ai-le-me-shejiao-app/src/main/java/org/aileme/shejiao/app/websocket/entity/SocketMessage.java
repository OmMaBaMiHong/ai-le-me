/**
 * -----------------------------------
 *  Copyright (c) 2021-2023

 * 版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.app.websocket.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息封装
 * @author linfeng
 * @date 2022/11/16 11:25
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SocketMessage <T>{

    private String type;

    private T data;
}
