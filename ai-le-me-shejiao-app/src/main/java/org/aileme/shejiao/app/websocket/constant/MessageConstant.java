package org.aileme.shejiao.app.websocket.constant; /**
 * -----------------------------------
 *  Copyright (c) 2021-2023
 *  All rights reserved, Designed By www.linfeng.tech
 * 林风社交论坛商业版本请务必保留此注释头信息
 * 商业版授权联系技术客服	 QQ:  3582996245
 * 严禁分享、盗用、转卖源码或非法牟利！
 * 版权所有 ，侵权必究！
 * -----------------------------------
 */

/**
 * IM消息常量
 * @author linfeng
 * @date 2022/11/16 11:28
 */
public class MessageConstant {

    /**私聊消息**/
    public static final String PERSON_MESSAGE = "person-message";

    /**撤回私聊消息**/
    public static final String PERSON_WITHDRAW = "person-withdraw";

    /**好友申请**/
    public static final String PERSON_APPLY = "person-apply";

    /**心跳机制**/
    public static final String PING = "ping";

    /**好友申请通过**/
    public static final String PERSON_APPLY_AGREE = "person-apply-agree";

    /**在线人数**/
    public static final String COUNT = "count";

    /**token失效**/
    public static final String TOKEN_FAILED = "token-failed";

    /**好友申请拒绝**/
    public static final String REJECT = "reject";

    /**好友申请拒绝**/
    public static final String NOTICE_REFRESH = "notice-refresh";

    /**IM会话缓存**/
    public static final String IM_CHAT = "IM_CHAT_";

    /**首次添加好友提示语**/
    public static final String DEFAULT_LAST_MESSAGE = "你们是好友啦~";

    /**消息 0未读**/
    public static final  Integer NOT_READ=0;

    /**消息 1已读**/
    public static final  Integer HAS_READ=1;

    /**推荐喜欢通知**/
    public static final String RECOMMEND_LOVE = "recommend-love";

    /**推荐不喜欢通知**/
    public static final String RECOMMEND_DISLIKE = "recommend-dislike";
}
