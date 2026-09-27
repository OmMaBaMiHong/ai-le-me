/**
 * -----------------------------------
 * Copyright (c) 2021-2024
 * All rights reserved, Designed By www.linfengtech.cn
 * 林风社交论坛商业版本请务必保留此注释头信息
 * 商业版授权联系技术客服	 QQ:  3582996245
 * 严禁分享、盗用、转卖源码或非法牟利！
 * 版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.common.utils;

/**
 * 常量
 *
 */
public class Constant {
    /** 超级管理员ID */
    public static final int SUPER_ADMIN = 1;
    /**
     * 当前页码
     */
    public static final String PAGE = "page";
    /**
     * 每页显示记录数
     */
    public static final String LIMIT = "limit";
    /**
     * 排序字段
     */
    public static final String ORDER_FIELD = "sidx";
    /**
     * 排序方式
     */
    public static final String ORDER = "order";
    /**
     *  升序
     */
    public static final String ASC = "asc";


    public static final String CLASSID = "classId";


    public static final String ORDER_DESC_ID = "id desc";
    public static final String ORDER_TYPE_ONE_TWO = "12";
    public static final String ORDER_TYPE_FOUR = "4";
    public static final String ORDER_TYPE_THREE = "3";


    public static final String ORDER_DESC_READCOUNT = "read_count desc";

    public static final Integer NOT_READ = 0;
    public static final Integer HAS_READ = 1;
    /**
     * 手机验证码长度
     */
    public static final Integer SMS_SIZE = 6;
    /**
     * 默认系统分配头像
     */
    public static final String DEAULT_HEAD = "https://pic.linfeng.tech/test/20220825/794e4d232bef4dcdac96497f9f487b48.jpeg";
    /**
     * 默认重置简介
     */
    public static final String DEAULT_INTRO = "暂无简介";
    /**
     * 默认标签
     */
    public static final String DEAULT_TAG = "萌新";
    /**
     * 默认官方圈子ID
     */
    public static final Integer OFFICIAL_TOPIC_ID = 1;
    /**
     * 付费贴封面图
     */
    public static final String DEAULT_VIP_POST = "https://pic.linfeng.tech/test/20220427/db39892a5d9e400bbc4f199dfa859b6f.png";

    /**
     * 默认分享海报封面图
     */
    public static final String DEAULT_SHARE_POST = "https://pic.linfeng.tech/test/20220825/f7fd813f92a24c0eb93484c3e08f7a62.png";

    /**
     * 圈子是否禁用
     */
    public static final Integer TOPIC_NORMAL = 0;
    public static final Integer TOPIC_BANNER = 1;

    /**
     * 0普通用户 1会员
     */
    public static final Integer VIP_USER = 1;
    public static final Integer COMMON_USER = 0;

    /**
     * 举报审核状态
     * 0待审核 1已处理 2已驳回
     */
    public static final Integer REPORT_REVIEW = 0;
    public static final Integer REPORT_FINISH = 1;
    public static final Integer REPORT_REFUSE = 2;
    /**
     * 敏感词是否开启审核
     */
    public static final Integer SENSITIVE_CLOSE = 0;
    public static final Integer SENSITIVE_OPEN = 1;
    /**
     * 敏感词处理措施 1-禁止发布 2-需审核
     */
    public static final Integer DEAL_BANNER = 1;
    public static final Integer DEAL_EXAMINE = 2;

    /**
     * 订单状态
     * 0 待支付
     * 1 已支付
     * 2 已退款
     * 3 已关闭
     */
    public static final Integer RECHARGE_STATUS_PENDING = 0;
    public static final Integer RECHARGE_STATUS_PAID = 1;
    public static final Integer RECHARGE_STATUS_REFUNDED = 2;
    public static final Integer RECHARGE_STATUS_CLOSED = 3;
    /**
     * 充值订单类型
     * 0 钱包余额充值(旧逻辑)
     * 1 会员充值
     * 2 爱情币充值
     */
    public static final Integer RECHARGE_ORDER_WALLET = 0;
    public static final Integer RECHARGE_ORDER_VIP = 1;
    public static final Integer RECHARGE_ORDER_COIN = 2;
    public static final Integer RECHARGE_ORDER_ACTIVITY = 3;

    public static final String ACCOUNT_ASSET_COIN = "coin";
    public static final String PAY_PRODUCT_TYPE_COIN = "coin";
    public static final String PAY_PRODUCT_TYPE_VIP = "vip";

    public static final String BILL_CATEGORY_ORDER = "order";
    public static final String BILL_EVENT_ORDER_CREATE = "order_create";
    public static final String BILL_EVENT_PAY_SUCCESS = "pay_success";
    public static final String BILL_EVENT_REFUND_SUCCESS = "refund_success";
    public static final String BILL_EVENT_COIN_REFUND = "coin_refund";
    /**
     * 通用状态 0正常 1禁止
     */
    public static final Integer NORMAL = 0;
    public static final Integer BAN = 1;

    public static final Integer TOPIC_AUDIT = 2;

    /**
     * 奖品类型
     * [1积分2谢谢惠顾3红包]
     */
    public static final Integer LUCK_INTEGRAL = 1;
    public static final Integer LUCK_THANKS = 2;
    public static final Integer LUCK_MONEY= 3;

    /**
     * 支付方式
     * 微信小程序支付 weixin
     * h5微信支付  h5
     * 微信公众号支付 wxh5
     */
    public static final String PAY_TYPE_WX = "weixin";
    public static final String PAY_TYPE_H5 = "h5";
    public static final String PAY_TYPE_APP = "app";
    public static final String PAY_TYPE_WXH5 = "wxh5";
    public static final String PAY_CHANNEL_WECHAT = "wechat";
    public static final String PAY_CHANNEL_ALIPAY = "alipay";
    public static final String PAY_MOCK_STATUS_SUCCESS = "success";
    public static final String PAY_MOCK_STATUS_FAIL = "fail";
    public static final String PAY_MOCK_STATUS_CANCEL = "cancel";


    public static final String WX = "weixin";
    public static final String H5 = "h5";
    public static final String APP = "app";

    /**
     * 微信支付返回状态码
     */
    public static final String WX_RETURN_CODE="return_code";
    /**
     * 微信支付成功标识
     */
    public static final String WX_PAY_SUCCESS="SUCCESS";
    /**
     * 提现申请状态
     * 0待审核 1已完成 2已驳回
     */
    public static final Integer CASH_REVIEWED = 0;
    public static final Integer CASH_FINISH = 1;
    public static final Integer CASH_REFUSE = 2;
    /**
     * 帖子状态
     * 0 正常
     * 1 待审核
     * 2 下架
     */
    public static final Integer POST_NORMAL = 0;
    public static final Integer POST_REVIEWED = 1;
    public static final Integer POST_BANNER = 2;
    /**
     * 帖子类型
     * 1 图文
     * 2 视频
     * 3 文章
     * 4 投票
     */
    public static final Integer POST_TYPE_TEXT = 1;
    public static final Integer POST_TYPE_VIDEO = 2;
    public static final Integer POST_TYPE_ARTICLE = 3;
    public static final Integer POST_TYPE_VOTE = 4;
    /**
     * 1为点赞，2为评论  3为收藏 4为关注  5为推送通知
     * 6私聊（6已废弃）
     */
    public static final Integer STAR = 1;
    public static final Integer COMMENT = 2;
    public static final Integer COLLECT = 3;
    public static final Integer WATCH = 4;
    public static final Integer PUSHARTICLE = 5;
    public static final Integer CHAT = 6;//6私聊（6已废弃,单独存储在lf_chat_message表）

    public static final String TITLE_WATCH = "关注通知";
    public static final String TITLE_COMMENT = "评论通知";
    public static final String TITLE_STAR = "点赞通知";
    public static final String TITLE_COLLECT = "点赞通知";
    public static final String TITLE_VIOLATION = "违规通知";
    public static final String TITLE_PAY = "付费通知";
    public static final String TITLE_PASS = "审核通知";
    public static final String TITLE_REPORT = "举报通知";
    public static final String TITLE_CASH = "提现通知";
    public static final String TITLE_TOPIC = "圈子通知";
    public static final String TITLE_REWARD = "打赏通知";

    public static final String CONTENT_NOTS = "用户【{}】给你发了小纸条";
    public static final String CONTENT_WATCH = "用户【{}】关注了你";
    public static final String CONTENT_STAR = "用户【{}】点赞了你的评论:{}";
    public static final String CONTENT_COMMENT = "用户【{}】评论了你的动态[{}]:{}";
    public static final String CONTENT_COMMENT_REPLY = "用户【{}】回复了你在动态[{}]下的评论:{}";
    public static final String CONTENT_POST_STAR = "用户【{}】点赞了你的动态:{}";
    public static final String ADMIN_POST_DOWN = "你的动态【{}】由于违反社区规定已被平台管理员删除";
    public static final String ADMIN_POST_DOWN_REASON = "你的动态【{}】已被平台管理员删除,原因如下：{}";
    public static final String TOPIC_ADMIN_POST_DOWN = "你的动态【{}】由于违反社区或圈子规定已被圈子【{}】管理员(ID为{})删除，圈子管理员与平台管理员无关，如有问题请联系该圈子管理员";
    public static final String VIP_POST = "用户【{}】支付查看了你的付费贴【{}】,您的账单:{}";
    public static final String ADMIN_POST_UP = "您的动态【{}】已通过审核";
    public static final String ADMIN_DOWN = "您的动态【{}】已被平台管理员下架，原因:【{}】";
    public static final String REPORT_NOTICE = "您举报的单号为【{}】的内容已被处理,平台管理员回复:{}";
    public static final String REPORT_NOTICE_REFUSE = "您举报的单号为【{}】的内容未发现异常,平台管理员回复:{}";
    public static final String CASH_OUT_REFUSE = "您的余额提现申请未通过审核，余额已解冻，原因如下:{}";
    public static final String CASH_OUT_PASS = "您的余额提现申请已通过审核，注意查看收款账户是否到账，如有疑问联系客服";
    public static final String TOPIC_CHANGE = "用户【{}】已将圈子【{}】的圈主权限转让至你的账户";
    public static final String TOPIC_APPLY_SUCCESS = "圈子【{}】通过了你的进圈申请";
    public static final String TOPIC_APPLY_FAIL = "圈子【{}】拒绝了你的进圈申请";
    public static final String REWARD_NOTICE = "用户【{}】打赏了你的帖子【{}】{}积分";


    //redis缓存配置值
    /**微信小程序Id **/
    public static final String WX_APP_ID = "WxAppId";
    /**微信小程序密钥 **/
    public static final String WX_APP_Secret = "wxAppSecret";
    /**微信公众号Id **/
    public static final String WX_MP_ID = "WxMpAppId";
    /**微信公众号密钥 **/
    public static final String WX_MP_Secret = "WxMpSecret";
    /**微信公众号验证码模板ID **/
    public static final String WX_MP_VERIFICATION_TEMPLATE_ID = "WxMpVerificationTemplateId";
    /**微信公众号验证码有效期文案 **/
    public static final String WX_MP_VERIFICATION_VALIDITY_TIME = "WxMpVerificationValidityTime";
    /**APP端的appId**/
    public static final String APP_APP_ID = "app_appid";
    /**微信支付回调地址 **/
    public static final String APP_NOTIFY_URL = "appNotifyurl";
    /**微信商户号 **/
    public static final String MCH_ID = "wxPayKey";
    /**微信商户密钥 **/
    public static final String KEY = "wxPaySecret";
    /**h5支付之后跳转地址 **/
    public static final String REDIRECT_URL = "redirectUrl";
    /**支付mock开关 **/
    public static final String PAY_MOCK_ENABLED = "pay.mock.enabled";
    /**支付宝AppId **/
    public static final String PAY_ALIPAY_APP_ID = "pay.alipay.appId";
    /**支付宝签名类型 **/
    public static final String PAY_ALIPAY_SIGN_TYPE = "pay.alipay.sign_type";
    /**支付宝应用私钥 **/
    public static final String PAY_ALIPAY_PRIVATE_KEY = "pay.alipay.privateKey";
    /**支付宝公钥 **/
    public static final String PAY_ALIPAY_PUBLIC_KEY = "pay.alipay.alipayPublicKey";
    /**支付宝回调地址 **/
    public static final String PAY_ALIPAY_NOTIFY_URL = "pay.alipay.notify_url";
    /**支付宝返回地址 **/
    public static final String PAY_ALIPAY_RETURN_URL = "pay.alipay.return_url";
    /**付费贴抽成% **/
    public static final String POST_PRICE = "postPrice";
    /**视频入口1开启 0关闭 **/
    public static final String IS_OPEN = "isOpen";
    /**积分兑换余额的开关0开启 1关闭**/
    public static final String EXCHANGE = "exchange";
    /**积分兑换余额的开关0开启 1关闭**/
    public static final String CHARGE = "chargeIsOpen";
    /**积分兑换比例**/
    public static final String INTEGRAL = "integral";
    /**普通贴是否需要人工审核 0人工审核 1自动过审**/
    public static final String NORMAL_POST = "normalPost";
    /**付费贴是否需要人工审核 0人工审核 1自动过审**/
    public static final String POST_VIP = "vipPost";
    /**阿里云短信开关0开启1关闭**/
    public static final String SMS_OPEN = "sms_open";
    /**项目logo**/
    public static final String IMG = "img";
    /**首页-最新-帖子列表样式 0普通样式 1瀑布流样式**/
    public static final String INDEX_STYLE_LAST = "indexStyle1";
    /**首页-圈子-帖子列表样式 0普通样式 1瀑布流样式**/
    public static final String INDEX_STYLE_TOPIC = "indexStyle2";
    /**首页-关注-帖子列表样式 0普通样式 1瀑布流样式**/
    public static final String INDEX_STYLE_WATCH = "indexStyle3";
    /**会员入口开关 0开启 1关闭**/
    public static final String VIP_SHOW = "vipShow";
    /**用户页面背景图**/
    public static final String BG_IMG = "bgImg";
    /**是否开启邮箱登录 0关闭 1开启**/
    public static final String EMAIL_LOGIN = "emailLogin";
    /**用户服务协议**/
    public static final String PROTOCOL = "protocol";
    /**个人隐私协议**/
    public static final String PRIVACY = "privacy";
    /**公告内容**/
    public static final String NOTICE_CONTENT = "noticeContent";
    /**客服微信号**/
    public static final String CONTACT_WECHAT = "contactWechat";
    /**客服微信二维码**/
    public static final String CONTACT_WECHAT_QR = "contactWechatQr";
    /**客服工作时间**/
    public static final String CONTACT_TIME = "contactTime";
    /**客服手机号**/
    public static final String CONTACT_PHONE = "contactPhone";
    /**每日抽奖次数**/
    public static final String SURPLUS = "surplus";
    /**抽奖是否开启 0=未开启  1=进行中**/
    public static final String LUCK_DRAW_STATUS = "luckDrawStatus";
    /**抽奖活动规则**/
    public static final String LUCK_DRAW_RULE = "luckDrawRule";
    /**每次抽奖消耗的积分数量**/
    public static final String LUCK_DRAW_INTEGRAL = "luckDrawIntegral";
    /**会员服务协议**/
    public static final String VIP_AGREE_CONTENT = "vipAgreeContent";
    /**会员积分奖励翻倍数**/
    public static final String VIP_INTEGRAL = "vipIntegral";
    /**发帖积分奖励**/
    public static final String ADD_POST_INTEGRAL = "addPostIntegral";
    /**每天限制发帖积分奖励次数**/
    public static final String ADD_POST_INTEGRAL_LIMIT = "addPostIntegralLimit";
    /**会员改名每月次数限制**/
    public static final String VIP_RENAME = "vipRename";
    /**普通用户改名每月次数限制**/
    public static final String COMMON_RENAME = "commonRename";
    /**会员创建圈子数量限制**/
    public static final String VIP_TOPIC_NUMBER = "vipTopicNumber";
    /**普通用户创建圈子数量限制**/
    public static final String COMMON_TOPIC_NUMBER = "commonTopicNumber";
    /**是否开启提现0关闭1开启**/
    public static final String CAN_CASH_OUT = "canCashOut";
    /**流量主广告开关0关闭1开启**/
    public static final String AD_IS_OPEN = "adIsOpen";
    /**流量主广告H5的adpid**/
    public static final String H5_AD_PID = "h5Adpid";
    /**流量主广告微信小程序的adpid**/
    public static final String WX_AD_PID = "wxAdpid";
    /**帖子详情页样式0格子布局1轮播布局**/
    public static final String POST_DETAIL_SHOW_TYPE = "showType";
    /**弹框公告是否开启0开启1关闭**/
    public static final String POPUP_OPEN = "popupOpen";
    /**首页弹框的标题**/
    public static final String POP_TITLE = "popTitle";
    /**首页弹框的内容**/
    public static final String POP_CONTENT = "popContent";
    /**首页弹框展示频率 单位天**/
    public static final String POP_TIME = "popTime";
    /**帖子详情页打赏按钮 0不显示 1显示**/
    public static final String REWARD_BTN = "rewardBtn";
    /**打赏抽成比例（百分比）**/
    public static final String REWARD_COMMISSION_RATE = "rewardCommissionRate";
    /**付费贴开关0开启1关闭**/
    public static final String PAY_POST_BTN = "payPostbtn";
    /**0所有人可发付费贴 1仅会员可发付费贴**/
    public static final String PAY_POST_VIP = "payPostVip";
    /**私密圈子创建入口0关闭1开启**/
    public static final String PRIVATE_CIRCLES_OPEN = "privateCirclesOpen";
    /**私密圈子创建0所有人可创建 1仅会员可创建**/
    public static final String PRIVATE_CIRCLES_NEED_VIP = "privateCircleNeedVip";
    /**评论发布审核  0直接过审 1需要审核**/
    public static final String COMMENT_CHECK = "commentCheck";
    /**邮箱注册 0关闭1开放**/
    public static final String OPEN_EMAIL_REGISTER = "openEmailRegister";
    /**圈子发布审核 0直接过审 2需要审核**/
    public static final String CIRCLE_CHECK = "circleCheck";
    /**IOS机型虚拟支付相关功能   0关闭 1开启**/
    public static final String IOS_CLOSE = "iosClose";
    /** 微信公众平台获取accessToken的url */
    public static final String WECHAT_OAUTH2_ACCESS_TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token?appid={}&secret={}&code={}&grant_type=authorization_code";


    /**短句请求开放接口**/
    public static final String ROBOT_CONTENT = "https://api.xygeng.cn/one";
    public static final String ROBOT_PIC = "https://api.btstu.cn/sjbz/api.php?lx=suiji&format=json";

    /**短句请求开放接口**/
  public static final String VIP_AD_BLOCK = "vip_ad_block";//广告屏蔽
    public static final String VIP_AI_CHAT ="vip_ai_chat" ;//ai聊天
    public static final String VIP_PAID_POST ="vip_paid_post" ;//付费贴



    /**
     * 菜单类型
     */
    public enum MenuType {
        /**
         * 目录
         */
        CATALOG(0),
        /**
         * 菜单
         */
        MENU(1),
        /**
         * 按钮
         */
        BUTTON(2);

        private int value;

        MenuType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * 定时任务状态
     */
    public enum ScheduleStatus {
        /**
         * 正常
         */
        NORMAL(0),
        /**
         * 暂停
         */
        PAUSE(1);

        private int value;

        ScheduleStatus(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

}
