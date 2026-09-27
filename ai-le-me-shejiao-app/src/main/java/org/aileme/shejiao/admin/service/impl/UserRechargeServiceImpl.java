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
package org.aileme.shejiao.admin.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aileme.shejiao.app.websocket.component.SocketServer;
import org.aileme.shejiao.app.websocket.constant.MessageConstant;
import org.aileme.shejiao.app.websocket.entity.SocketMessage;
import org.aileme.shejiao.common.enums.BillDetailEnum;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.domain.vo.OrderPay;
import org.aileme.shejiao.common.utils.*;
import org.aileme.shejiao.admin.utils.WechatUtil;
import org.aileme.shejiao.domain.entity.admin.*;
import org.aileme.shejiao.api.service.*;
import org.aileme.shejiao.domain.entity.app.ChatMessageEntity;
import org.aileme.shejiao.domain.param.app.PayVipPostForm;
import org.aileme.shejiao.common.utils.weixin.sdk.PaymentApi;
import org.aileme.shejiao.common.utils.weixin.sdk.PaymentKit;
import org.aileme.shejiao.common.utils.weixin.sdk.WXPayUtil;
import org.aileme.shejiao.api.service.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.aileme.shejiao.admin.dao.UserRechargeDao;
import org.aileme.shejiao.admin.dao.UserRechargeRefundDao;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;


@DS("master")
@Service("userRechargeService")
@Slf4j
public class UserRechargeServiceImpl extends ServiceImpl<UserRechargeDao, UserRechargeEntity> implements UserRechargeService {

    private static final String ALIPAY_GATEWAY = "https://openapi.alipay.com/gateway.do";
    private static final DateTimeFormatter ALIPAY_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private AppUserService userService;
    @Autowired
    private BillService billService;
    @Autowired
    private PostService postService;
    @Autowired
    private MessageService messageService;
    @Autowired
    private SysConfigService configService;
    @Autowired
    private UserRechargeDao userRechargeDao;
    @Autowired
    private VipOptionService vipOptionService;
    @Autowired
    private RechargeService rechargeService;
    @Autowired
    private UserRechargeRefundDao userRechargeRefundDao;
    @Autowired
    private AccountService accountService;
    @Autowired
    private PayOrderDetailService payOrderDetailService;

    @Autowired
    private WechatRuntimeConfigService wechatRuntimeConfigService;
    @Autowired
    private PaymentRuntimeConfigService paymentRuntimeConfigService;
    @Autowired
    private XiangqinEnrollmentService xiangqinEnrollmentService;
    @Autowired
    private XiangqinActivityService xiangqinActivityService;
    @Autowired
    private HongniangService hongniangService;
    @Autowired
    private FriendService friendService;
    @Autowired
    private ChatMessageService chatMessageService;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {

        QueryWrapper<UserRechargeEntity> queryWrapper=new QueryWrapper<>();
        //条件查询
        String key = (String) params.get("key");
        String type = (String) params.get("type");
        String type2 = (String) params.get("type2");

        if (NumberUtil.isInteger(key)) {
            if (!WechatUtil.isEmpty(key)) {
                queryWrapper.lambda().like(UserRechargeEntity::getUid,Integer.valueOf(key));
            }
        } else {
            if (!WechatUtil.isEmpty(key)) {
                queryWrapper.lambda()
                        .and(wrapper -> wrapper.like(UserRechargeEntity::getOrderId, key)
                                .or()
                                .like(UserRechargeEntity::getNickname, key));
            }
        }
        if (!WechatUtil.isEmpty(type)) {
            queryWrapper.lambda().eq(UserRechargeEntity::getStatus, Integer.valueOf(type));
        }
        if (!WechatUtil.isEmpty(type2)) {
            queryWrapper.lambda().eq(UserRechargeEntity::getType,Integer.valueOf(type2));
        }
        queryWrapper.lambda().orderByDesc(UserRechargeEntity::getId);
        IPage<UserRechargeEntity> page = this.page(
                new Query<UserRechargeEntity>().getPage(params),
                queryWrapper
        );
        return new PageUtils(page);
    }

    @Override
    public String addRecharge(AppUserEntity user, String price, String paidPrice,String type) {
        return addRecharge(user, price, paidPrice, type, Constant.RECHARGE_ORDER_WALLET);
    }

    @Override
    public String addRecharge(AppUserEntity user, String price, String paidPrice, String payType, Integer orderType) {
        UserRechargeEntity entity=new UserRechargeEntity();
        String orderSn = IdUtil.createSnowflake(0L, 0).nextIdStr();

        entity.setNickname(user.getUsername());
        entity.setOrderId(orderSn);
        entity.setTitle(resolveOrderTitle(orderType));
        entity.setUid(user.getUid());
        entity.setPrice(new BigDecimal(price));
        entity.setGivePrice(new BigDecimal(paidPrice));
        entity.setCoinAmount(calculateCoinAmount(new BigDecimal(price), new BigDecimal(paidPrice)));
        entity.setRechargeType(payType);
        entity.setChannel(Constant.PAY_CHANNEL_WECHAT);
        entity.setStatus(Constant.RECHARGE_STATUS_PENDING);
        entity.setAddTime(DateUtil.nowDateTime());
        entity.setUpdateTime(DateUtil.nowDateTime());
        entity.setType(orderType);
        this.save(entity);
        addOrderEventLog(entity, Constant.BILL_EVENT_ORDER_CREATE, "订单创建");
        return orderSn;
    }

    @Override
    public String addRecharge(AppUserEntity user, RechargeEntity product, String payType, String payChannel, Integer orderType) {
        if (ObjectUtil.isNull(product)) {
            throw new LinfengException("充值商品不存在");
        }
        UserRechargeEntity entity = new UserRechargeEntity();
        String orderSn = IdUtil.createSnowflake(0L, 0).nextIdStr();
        BigDecimal price = normalizeAmount(product.getPrice());
        BigDecimal giftAmount = normalizeAmount(product.getGivePrice());
        entity.setNickname(user.getUsername());
        entity.setOrderId(orderSn);
        entity.setTitle("购买爱情币");
        entity.setUid(user.getUid());
        entity.setBizId(String.valueOf(product.getId()));
        entity.setPrice(price);
        entity.setGivePrice(giftAmount);
        entity.setCoinAmount(calculateCoinAmount(price, giftAmount));
        entity.setRechargeType(payType);
        entity.setChannel(normalizePayChannel(payChannel));
        entity.setStatus(Constant.RECHARGE_STATUS_PENDING);
        entity.setAddTime(DateUtil.nowDateTime());
        entity.setUpdateTime(DateUtil.nowDateTime());
        entity.setType(orderType);
        this.save(entity);
        addOrderEventLog(entity, Constant.BILL_EVENT_ORDER_CREATE, "订单创建");
        return orderSn;
    }

    @Override
    @DSTransactional
    public String addActivityOrder(AppUserEntity user, XiangqinEnrollmentEntity enrollment, XiangqinActivityEntity activity,
                                   String payType, String payChannel) {
        if (ObjectUtil.isNull(user) || ObjectUtil.isNull(enrollment) || ObjectUtil.isNull(activity)) {
            throw new LinfengException("活动报名订单参数不完整");
        }
        String bizId = String.valueOf(enrollment.getId());
        UserRechargeEntity pendingOrder = this.lambdaQuery()
                .eq(UserRechargeEntity::getUid, user.getUid())
                .eq(UserRechargeEntity::getType, Constant.RECHARGE_ORDER_ACTIVITY)
                .eq(UserRechargeEntity::getBizId, bizId)
                .eq(UserRechargeEntity::getStatus, Constant.RECHARGE_STATUS_PENDING)
                .orderByDesc(UserRechargeEntity::getId)
                .last("limit 1")
                .one();
        if (pendingOrder != null) {
            pendingOrder.setTitle(buildActivityOrderTitle(activity));
            pendingOrder.setPrice(normalizeAmount(enrollment.getPaymentAmount()));
            pendingOrder.setGivePrice(BigDecimal.ZERO);
            pendingOrder.setCoinAmount(0);
            pendingOrder.setRechargeType(payType);
            pendingOrder.setChannel(normalizePayChannel(payChannel));
            pendingOrder.setRemark(String.valueOf(activity.getId()));
            pendingOrder.setUpdateTime(DateUtil.nowDateTime());
            this.updateById(pendingOrder);
            return pendingOrder.getOrderId();
        }

        UserRechargeEntity entity = new UserRechargeEntity();
        String orderSn = IdUtil.createSnowflake(0L, 0).nextIdStr();
        entity.setNickname(user.getUsername());
        entity.setOrderId(orderSn);
        entity.setTitle(buildActivityOrderTitle(activity));
        entity.setUid(user.getUid());
        entity.setBizId(bizId);
        entity.setPrice(normalizeAmount(enrollment.getPaymentAmount()));
        entity.setGivePrice(BigDecimal.ZERO);
        entity.setCoinAmount(0);
        entity.setRechargeType(payType);
        entity.setChannel(normalizePayChannel(payChannel));
        entity.setStatus(Constant.RECHARGE_STATUS_PENDING);
        entity.setAddTime(DateUtil.nowDateTime());
        entity.setUpdateTime(DateUtil.nowDateTime());
        entity.setType(Constant.RECHARGE_ORDER_ACTIVITY);
        entity.setRemark(String.valueOf(activity.getId()));
        this.save(entity);
        addOrderEventLog(entity, Constant.BILL_EVENT_ORDER_CREATE, "订单创建");
        return orderSn;
    }

    @Override
    public OrderPay goToPay(String orderSn, Double price,String ip,AppUserEntity user,String type) throws Exception {
        return goToPay(orderSn, price, ip, user, type, Constant.PAY_CHANNEL_WECHAT, null);
    }

    @Override
    public OrderPay goToPay(String orderSn, Double price, String ip, AppUserEntity user, String type, String payChannel, String h5Origin) throws Exception {
        String channel = normalizePayChannel(payChannel);
        if (Constant.PAY_TYPE_H5.equals(type) && Constant.PAY_CHANNEL_ALIPAY.equals(channel) && !shouldUseMockPay(channel)) {
            return buildAlipayWapOrderPay(orderSn, price, h5Origin);
        }
        if (Constant.PAY_TYPE_H5.equals(type) && shouldUseMockPay(channel)) {
            return buildMockOrderPay(orderSn, channel, h5Origin);
        }
        String appNotifyUrl = wechatRuntimeConfigService.getWechatPayNotifyUrl();
        String mchId = wechatRuntimeConfigService.getWechatPayMchId();
        String key = wechatRuntimeConfigService.getWechatPayApiKey();
        validateRequiredConfig(appNotifyUrl, "微信支付回调地址");
        validateRequiredConfig(mchId, "微信支付商户号");
        validateRequiredConfig(key, "微信支付商户密钥");

        WechatPayRequestContext payContext = buildWechatPayRequestContext(type, user, h5Origin);

        BigDecimal money = new BigDecimal(price);
        Map<String, String> reqParams = new HashMap<>();
        String outTradeNo=orderSn+"-"+ WechatUtil.getRandomCode(3,0)+"PAY";

        reqParams.put("appid", payContext.getAppId());
        reqParams.put("trade_type", payContext.getTradeType());
        if (StrUtil.isNotBlank(payContext.getOpenid())) {
            reqParams.put("openid", payContext.getOpenid());
        }
        if (StrUtil.isNotBlank(payContext.getSceneInfo())) {
            reqParams.put("scene_info", payContext.getSceneInfo());
        }
        reqParams.put("mch_id", mchId);
        reqParams.put("nonce_str", System.currentTimeMillis() / 1000 + "");
        reqParams.put("sign_type", "MD5");
        reqParams.put("body", "充值"+orderSn+"订单-微信支付");
        reqParams.put("out_trade_no", outTradeNo);
        reqParams.put("total_fee", money.multiply(BigDecimal.valueOf(100)).intValue() + "");
        reqParams.put("spbill_create_ip", StrUtil.blankToDefault(ip, "127.0.0.1"));
        reqParams.put("notify_url", appNotifyUrl);
        String sign = WXPayUtil.generateSignature(reqParams, key);
        reqParams.put("sign", sign);
        log.info(JSON.toJSONString(reqParams));
        String xmlResult = PaymentApi.pushOrder(reqParams);
        log.info(xmlResult);
        Map<String, String> result = PaymentKit.xmlToMap(xmlResult);

        String returnCode = result.get(Constant.WX_RETURN_CODE);
        String resultCode = result.get("result_code");
        if (!Constant.WX_PAY_SUCCESS.equalsIgnoreCase(returnCode) || !Constant.WX_PAY_SUCCESS.equalsIgnoreCase(resultCode)) {
            String errMsg = StrUtil.firstNonBlank(result.get("err_code_des"), result.get("return_msg"), result.get("return_code"), "微信统一下单失败");
            throw new LinfengException(errMsg);
        }

        String prepayId = result.get("prepay_id");
        Map<String, String> packageParams = new HashMap<>();
        packageParams.put("appId", reqParams.get("appid"));
        packageParams.put("timeStamp", System.currentTimeMillis() / 1000 + "");
        packageParams.put("nonceStr", System.currentTimeMillis() + "");
        packageParams.put("signType", "MD5");
        packageParams.put("codeUrl", result.get("code_url"));

        if (Constant.PAY_TYPE_H5.equals(type)) {
            String mwebUrl = result.get("mweb_url");
            if (StrUtil.isBlank(mwebUrl)) {
                throw new LinfengException("微信H5支付下单失败，缺少 mweb_url");
            }
            String redirectUrl = resolveWechatH5RedirectUrl(h5Origin);
            packageParams.put("mwebUrl", mwebUrl + "&redirect_url=" + urlEncode(redirectUrl));
        } else {
            if (StrUtil.isBlank(prepayId)) {
                throw new LinfengException("微信支付下单失败，缺少 prepay_id");
            }
            packageParams.put("package", "prepay_id=" + prepayId);
            String packageSign = WXPayUtil.generateSignature(packageParams, key);
            packageParams.put("paySign", packageSign);
            if (Constant.PAY_TYPE_APP.equals(type)) {
                packageParams.put("partnerId", mchId);
            }
        }
        log.info("支付返回结果集:{}" ,packageParams.toString());
        ObjectMapper mapper = new ObjectMapper();
        OrderPay pay = mapper.readValue(mapper.writeValueAsString(packageParams), OrderPay.class);
        pay.setPayChannel(channel);
        pay.setMockMode(Boolean.FALSE);
        pay.setOrderId(orderSn);

        return pay;
    }

    /**
     * 处理支付回调后业务
     * @param orderId
     * @param transaction_id
     * @param orderNo
     */
    @Override
    public void handleWxLog(String orderId, String transaction_id, String orderNo) {
        //1.更新订单信息
        UserRechargeEntity userRecharge = this.lambdaQuery()
                .eq(UserRechargeEntity::getOrderId, orderId)
                .one();
        if(ObjectUtil.isNull(userRecharge)){
            throw new LinfengException("该订单号不存在");
        }
        if (Constant.RECHARGE_STATUS_PAID.equals(userRecharge.getStatus())
                || Constant.RECHARGE_STATUS_REFUNDED.equals(userRecharge.getStatus())) {
            log.info("订单已处理，忽略重复支付回调 orderId={}", orderId);
            return;
        }
        userRecharge.setPayTime(DateUtil.nowDateTime());
        userRecharge.setStatus(Constant.RECHARGE_STATUS_PAID);
        userRecharge.setTransactionId(transaction_id);
        userRecharge.setOutTradeNo(orderNo);
        userRecharge.setUpdateTime(DateUtil.nowDateTime());
        boolean b = this.saveOrUpdate(userRecharge);
        if(!b){
            throw new LinfengException("订单更新失败");
        }
        addOrderEventLog(userRecharge, Constant.BILL_EVENT_PAY_SUCCESS, "支付成功");
        Integer rechargeOrderType = userRecharge.getType() == null ? Constant.RECHARGE_ORDER_WALLET : userRecharge.getType();
        if(Constant.RECHARGE_ORDER_ACTIVITY.equals(rechargeOrderType)){
            confirmActivityEnrollment(userRecharge);
        }else if(Constant.RECHARGE_ORDER_VIP.equals(rechargeOrderType)){
            vipOptionService.rollBackVip(userRecharge);
        }else if (Constant.RECHARGE_ORDER_COIN.equals(rechargeOrderType)) {
            creditIntegral(userRecharge);
        }else{
            BigDecimal money=userService.updateMoney(userRecharge.getUid(),userRecharge.getPrice(),userRecharge.getGivePrice());
            double v = userRecharge.getPrice().add(userRecharge.getGivePrice()).doubleValue();
            String mark="用户充值"+userRecharge.getPrice()+"赠"+userRecharge.getGivePrice()+"元";
            billService.record(userRecharge.getUid(), 1, BillDetailEnum.TYPE_1.getDesc(),
                    BillDetailEnum.CATEGORY_1.getValue(), BillDetailEnum.TYPE_1.getValue(), v, money.doubleValue(),
                    mark, userRecharge.getBizId(), userRecharge.getOrderId(), null, 1);

        }

    }

    @Override
    public void handleMockPay(String orderId, String payChannel, String status) {
        String mockStatus = StrUtil.blankToDefault(status, Constant.PAY_MOCK_STATUS_SUCCESS);
        if (!Constant.PAY_MOCK_STATUS_SUCCESS.equalsIgnoreCase(mockStatus)) {
            return;
        }
        UserRechargeEntity userRecharge = this.lambdaQuery()
                .eq(UserRechargeEntity::getOrderId, orderId)
                .one();
        if (ObjectUtil.isNull(userRecharge)) {
            throw new LinfengException("该订单号不存在");
        }
        if (Constant.RECHARGE_STATUS_PAID.equals(userRecharge.getStatus())
                || Constant.RECHARGE_STATUS_REFUNDED.equals(userRecharge.getStatus())) {
            return;
        }
        String channel = normalizePayChannel(payChannel);
        String transactionId = "mock_" + channel + "_" + System.currentTimeMillis();
        String orderNo = orderId + "-MOCK" + channel.toUpperCase();
        handleWxLog(orderId, transactionId, orderNo);
    }

    private void creditIntegral(UserRechargeEntity userRecharge) {
        int rechargeIntegral = userRecharge.getCoinAmount() == null || userRecharge.getCoinAmount() <= 0
                ? calculateCoinAmount(userRecharge.getPrice(), userRecharge.getGivePrice())
                : userRecharge.getCoinAmount();
        BigDecimal totalIntegral = accountService.increaseCoin(userRecharge.getUid(), rechargeIntegral);
        org.aileme.common.redis.utils.RedisUtils.deleteObject("userId:" + userRecharge.getUid());
        String mark = "购买爱情币" + userRecharge.getPrice() + "元赠" + userRecharge.getGivePrice()
                + "元，到账" + rechargeIntegral + "爱情币";
        billService.record(userRecharge.getUid(), 1, BillDetailEnum.TYPE_21.getDesc(),
                BillDetailEnum.CATEGORY_2.getValue(), BillDetailEnum.TYPE_21.getValue(),
                rechargeIntegral, totalIntegral.doubleValue(), mark, userRecharge.getBizId(), userRecharge.getOrderId(), null, 1);
    }

    private int calculateCoinAmount(BigDecimal price, BigDecimal givePrice) {
        BigDecimal integralRate = new BigDecimal(configService.getValue(Constant.INTEGRAL));
        BigDecimal totalAmount = normalizeAmount(price).add(normalizeAmount(givePrice));
        return totalAmount.multiply(integralRate)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    private boolean shouldUseMockPay(String payChannel) {
        String mockEnabled = configService.getValue(Constant.PAY_MOCK_ENABLED);
        if (StrUtil.isNotBlank(mockEnabled)) {
            return Boolean.parseBoolean(mockEnabled);
        }
        if (Constant.PAY_CHANNEL_ALIPAY.equals(payChannel)) {
            return !isAlipayConfigured();
        }
        return false;
    }

    private boolean isAlipayConfigured() {
        return paymentRuntimeConfigService.isAlipayConfigured();
    }

    private OrderPay buildMockOrderPay(String orderSn, String payChannel, String h5Origin) {
        UserRechargeEntity userRecharge = this.lambdaQuery()
                .eq(UserRechargeEntity::getOrderId, orderSn)
                .one();
        if (ObjectUtil.isNull(userRecharge)) {
            throw new LinfengException("充值订单不存在");
        }
        String origin = resolveH5Origin(h5Origin);
        String bizType = Constant.RECHARGE_ORDER_VIP.equals(userRecharge.getType()) ? "vip"
                : (Constant.RECHARGE_ORDER_ACTIVITY.equals(userRecharge.getType()) ? "activity" : "coin");
        String returnPage;
        if (Constant.RECHARGE_ORDER_VIP.equals(userRecharge.getType())) {
            returnPage = "/pages/user/vip/vip";
        } else if (Constant.RECHARGE_ORDER_ACTIVITY.equals(userRecharge.getType())) {
            returnPage = resolveActivityReturnPage(userRecharge);
        } else {
            returnPage = "/subpages/account/account";
        }
        String mockUrl = origin + "/#/pages/pay/mock"
                + "?orderId=" + orderSn
                + "&payChannel=" + urlEncode(payChannel)
                + "&bizType=" + bizType
                + "&returnPage=" + urlEncode(returnPage);
        OrderPay pay = new OrderPay();
        pay.setOrderId(orderSn);
        pay.setPayChannel(payChannel);
        pay.setMockMode(Boolean.TRUE);
        pay.setMwebUrl(mockUrl);
        pay.setPackages("mock_order");
        return pay;
    }

    private OrderPay buildAlipayWapOrderPay(String orderSn, Double price, String h5Origin) {
        UserRechargeEntity userRecharge = this.lambdaQuery()
                .eq(UserRechargeEntity::getOrderId, orderSn)
                .one();
        if (ObjectUtil.isNull(userRecharge)) {
            throw new LinfengException("充值订单不存在");
        }
        String subject = buildAlipaySubject(userRecharge);
        String notifyUrl = resolveAlipayNotifyUrl();
        String returnUrl = resolveAlipayReturnUrl(userRecharge, h5Origin);
        String signType = paymentRuntimeConfigService.getAlipaySignType();
        String privateKey = paymentRuntimeConfigService.getAlipayPrivateKey();

        Map<String, String> params = new LinkedHashMap<>();
        params.put("app_id", paymentRuntimeConfigService.getAlipayAppId());
        params.put("method", "alipay.trade.wap.pay");
        params.put("format", "JSON");
        params.put("charset", "UTF-8");
        params.put("sign_type", signType);
        params.put("timestamp", LocalDateTime.now().format(ALIPAY_TIMESTAMP_FORMATTER));
        params.put("version", "1.0");
        params.put("notify_url", notifyUrl);
        params.put("return_url", returnUrl);
        params.put("biz_content", JSON.toJSONString(buildAlipayBizContent(orderSn, price, subject)));
        String sign = AlipaySignUtils.sign(params, privateKey);
        params.put("sign", sign);

        StringBuilder urlBuilder = new StringBuilder(ALIPAY_GATEWAY).append("?");
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                urlBuilder.append("&");
            }
            first = false;
            urlBuilder.append(entry.getKey())
                    .append("=")
                    .append(urlEncode(entry.getValue()));
        }

        OrderPay pay = new OrderPay();
        pay.setOrderId(orderSn);
        pay.setPayChannel(Constant.PAY_CHANNEL_ALIPAY);
        pay.setMockMode(Boolean.FALSE);
        pay.setMwebUrl(urlBuilder.toString());
        pay.setSignType(signType);
        return pay;
    }

    private Map<String, Object> buildAlipayBizContent(String orderSn, Double price, String subject) {
        Map<String, Object> bizContent = new LinkedHashMap<>();
        bizContent.put("out_trade_no", orderSn);
        bizContent.put("total_amount", BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP).toPlainString());
        bizContent.put("subject", subject);
        bizContent.put("product_code", "QUICK_WAP_WAY");
        return bizContent;
    }

    private String buildAlipaySubject(UserRechargeEntity userRecharge) {
        if (Constant.RECHARGE_ORDER_VIP.equals(userRecharge.getType())) {
            return "爱了么VIP会员开通";
        }
        if (Constant.RECHARGE_ORDER_COIN.equals(userRecharge.getType())) {
            return "爱了么爱情币充值";
        }
        if (Constant.RECHARGE_ORDER_ACTIVITY.equals(userRecharge.getType())) {
            return StrUtil.blankToDefault(userRecharge.getTitle(), "爱了么活动报名");
        }
        return "爱了么账户充值";
    }

    private String resolveAlipayNotifyUrl() {
        return paymentRuntimeConfigService.getAlipayNotifyUrl();
    }

    private String resolveAlipayReturnUrl(UserRechargeEntity userRecharge, String h5Origin) {
        String configured = paymentRuntimeConfigService.getAlipayReturnUrl();
        if (StrUtil.isNotBlank(configured)) {
            return configured;
        }
        String origin = resolveH5Origin(h5Origin);
        if (Constant.RECHARGE_ORDER_VIP.equals(userRecharge.getType())) {
            return origin + "/#/pages/user/vip/vip?payStatus=success";
        }
        if (Constant.RECHARGE_ORDER_ACTIVITY.equals(userRecharge.getType())) {
            String activityPage = resolveActivityReturnPage(userRecharge);
            String connector = activityPage.contains("?") ? "&" : "?";
            return origin + "/#" + activityPage + connector + "payStatus=success";
        }
        return origin + "/#/subpages/account/account?payStatus=success";
    }

    private String resolveH5Origin(String h5Origin) {
        if (StrUtil.isNotBlank(h5Origin)) {
            return StrUtil.removeSuffix(h5Origin, "/");
        }
        String redirectUrl = paymentRuntimeConfigService.getWechatH5RedirectUrl();
        String configuredOrigin = extractOrigin(redirectUrl);
        if (StrUtil.isNotBlank(configuredOrigin)) {
            return configuredOrigin;
        }
        if (StrUtil.isBlank(redirectUrl)) {
            return "http://localhost:5173";
        }
        return extractOrigin(redirectUrl);
    }

    private String resolveWechatH5RedirectUrl(String h5Origin) {
        String configured = paymentRuntimeConfigService.getWechatH5RedirectUrl();
        if (StrUtil.isNotBlank(configured)) {
            return normalizeRedirectUrl(configured, resolveH5Origin(h5Origin));
        }
        return resolveH5Origin(h5Origin);
    }

    private String normalizeRedirectUrl(String configured, String fallbackOrigin) {
        String trimmed = StrUtil.trim(configured);
        if (StrUtil.isBlank(trimmed)) {
            return fallbackOrigin;
        }
        if (StrUtil.startWithAnyIgnoreCase(trimmed, "http://", "https://")) {
            return StrUtil.removeSuffix(trimmed, "/");
        }
        if (trimmed.startsWith("//")) {
            return "https:" + StrUtil.removeSuffix(trimmed, "/");
        }
        return "https://" + StrUtil.removePrefix(StrUtil.removeSuffix(trimmed, "/"), "//");
    }

    private String extractOrigin(String configuredUrl) {
        String normalized = normalizeRedirectUrl(configuredUrl, "http://localhost:5173");
        int hashIndex = normalized.indexOf("/#");
        if (hashIndex > 0) {
            normalized = normalized.substring(0, hashIndex);
        }
        if (!StrUtil.startWithAnyIgnoreCase(normalized, "http://", "https://")) {
            return StrUtil.removeSuffix(normalized, "/");
        }
        int slashIndex = normalized.indexOf('/', normalized.indexOf("://") + 3);
        if (slashIndex > 0) {
            normalized = normalized.substring(0, slashIndex);
        }
        return StrUtil.removeSuffix(normalized, "/");
    }

    private String normalizePayChannel(String payChannel) {
        if (Constant.PAY_CHANNEL_ALIPAY.equalsIgnoreCase(payChannel)) {
            return Constant.PAY_CHANNEL_ALIPAY;
        }
        return Constant.PAY_CHANNEL_WECHAT;
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(StrUtil.blankToDefault(value, ""), "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private WechatPayRequestContext buildWechatPayRequestContext(String type, AppUserEntity user, String h5Origin) {
        String miniAppId = wechatRuntimeConfigService.getMiniAppId();
        String mpAppId = wechatRuntimeConfigService.getMpAppId();
        String appAppId = wechatRuntimeConfigService.getAppAppId();

        if (Constant.PAY_TYPE_WX.equals(type)) {
            validateRequiredConfig(miniAppId, "微信小程序AppId");
            if (user == null || StrUtil.isBlank(user.getOpenid())) {
                throw new LinfengException("当前用户未绑定小程序 openid，无法发起微信支付");
            }
            return new WechatPayRequestContext(miniAppId, "JSAPI", user.getOpenid(), null);
        }
        if (Constant.PAY_TYPE_WXH5.equals(type)) {
            validateRequiredConfig(mpAppId, "微信公众号AppId");
            if (user == null || StrUtil.isBlank(user.getMpOpenid())) {
                throw new LinfengException("当前用户未绑定公众号 openid，无法发起公众号支付");
            }
            return new WechatPayRequestContext(mpAppId, "JSAPI", user.getMpOpenid(), null);
        }
        if (Constant.PAY_TYPE_H5.equals(type)) {
            validateRequiredConfig(mpAppId, "微信公众号AppId");
            String origin = resolveH5Origin(h5Origin);
            Map<String, Object> h5Info = new LinkedHashMap<>();
            h5Info.put("type", "Wap");
            h5Info.put("wap_url", origin);
            h5Info.put("wap_name", "爱了么");
            Map<String, Object> sceneInfo = new LinkedHashMap<>();
            sceneInfo.put("h5_info", h5Info);
            return new WechatPayRequestContext(mpAppId, "MWEB", null, JSON.toJSONString(sceneInfo));
        }
        if (Constant.PAY_TYPE_APP.equals(type)) {
            validateRequiredConfig(appAppId, "App支付AppId");
            return new WechatPayRequestContext(appAppId, "APP", null, null);
        }
        throw new LinfengException("暂不支持的支付方式:" + type);
    }

    private void validateRequiredConfig(String value, String label) {
        if (StrUtil.isBlank(value)) {
            throw new LinfengException(label + "未配置");
        }
    }

    private static final class WechatPayRequestContext {
        private final String appId;
        private final String tradeType;
        private final String openid;
        private final String sceneInfo;

        private WechatPayRequestContext(String appId, String tradeType, String openid, String sceneInfo) {
            this.appId = appId;
            this.tradeType = tradeType;
            this.openid = openid;
            this.sceneInfo = sceneInfo;
        }

        private String getAppId() {
            return appId;
        }

        private String getTradeType() {
            return tradeType;
        }

        private String getOpenid() {
            return openid;
        }

        private String getSceneInfo() {
            return sceneInfo;
        }
    }

    /**
     * 支付付费贴
     *
     * @param param
     * @param users
     */
    @Override
    @DSTransactional
    public void payVipPost(PayVipPostForm param, AppUserEntity users) {
        String price = configService.getValue(Constant.POST_PRICE);
        //这里安全起见，不用redis缓存取出的用户余额信息
        AppUserEntity user = userService.getById(users.getUid());
        //判断是否支付
        boolean isPay = billService.vipPostIsPay(param.getPostId(), user.getUid());
        if(isPay){
            throw new LinfengException("请勿重复支付");
        }
        //判断余额是否足够
        PostEntity post = postService.getById(param.getPostId());
        if(post.getPay().compareTo(user.getMoney()) > 0){
            throw new LinfengException("余额不足");
        }
        //余额支付
        BigDecimal balance = user.getMoney().subtract(post.getPay());

        boolean update = userService.lambdaUpdate().set(AppUserEntity::getMoney, balance)
                .eq(AppUserEntity::getUid, user.getUid())
                .update();
        if(!update){
            throw new LinfengException("余额扣除失败");
        }
        //删除用户信息缓存
        org.aileme.common.redis.utils.RedisUtils.deleteObject("userId:"+user.getUid());
        //记录账单
        String mark="付费贴支付"+post.getPay()+"元";
        billService.expend(user.getUid(),BillDetailEnum.TYPE_3.getDesc(),BillDetailEnum.CATEGORY_1.getValue(),BillDetailEnum.TYPE_3.getValue(),post.getPay().doubleValue(),balance.doubleValue(),mark,post.getId().toString(), null);
        //发布方收取费用
        AppUserEntity author = userService.getById(post.getUid());
        boolean integer = NumberUtil.isInteger(price);
        if(!integer){
            throw new LinfengException("抽成配置不是整数");
        }
        Integer value = Integer.valueOf(price);
        //抽成
        BigDecimal multiply = new BigDecimal(value).multiply(new BigDecimal(0.01));
        //用户获取的金额+原来的余额=用户总余额
        BigDecimal add = author.getMoney().add(post.getPay().multiply(multiply));
        boolean authorUpdate = userService.lambdaUpdate().set(AppUserEntity::getMoney, add)
                .eq(AppUserEntity::getUid, post.getUid())
                .update();
        if(!authorUpdate){
            throw new LinfengException("余额新增失败");
        }
        //删除发布方用户信息缓存
        org.aileme.common.redis.utils.RedisUtils.deleteObject("userId:"+post.getUid());
        //发布方记录账单
        String authorMark="付费贴("+post.getPay()+"元)(提成"+price+"%)入账"+post.getPay().multiply(multiply).setScale(2,BigDecimal.ROUND_HALF_UP)+"元";
        billService.income(post.getUid(),BillDetailEnum.TYPE_2.getDesc(),BillDetailEnum.CATEGORY_1.getValue(),BillDetailEnum.TYPE_2.getValue(),post.getPay().multiply(multiply).doubleValue(),add.doubleValue(),authorMark,post.getId().toString(), user.getUid());
        //通知发布方
        String content = StrUtil.format(Constant.VIP_POST,user.getUsername(),post.getTitle(),authorMark);
        messageService.sendMessageNotAsync(0,author.getUid(),post.getId(),Constant.PUSHARTICLE,content,Constant.TITLE_PAY);
    }

    /**
     * 用户充值总金额
     * @return
     */
    @Override
    public double rechargeMoney() {

        return userRechargeDao.rechargeMoney();
    }

    /**
     * 本月充值总金额
     * @return
     */
    @Override
    public double rechargeMoneyByMonth() {
        Date nowMonth = cn.hutool.core.date.DateUtil.beginOfMonth(new Date());
        LambdaQueryWrapper<UserRechargeEntity> wrapper=new LambdaQueryWrapper<>();
        wrapper.eq(UserRechargeEntity::getStatus, Constant.RECHARGE_STATUS_PAID)
                .ge(UserRechargeEntity::getPayTime,nowMonth);

        return userRechargeDao.rechargeMoneyByMonth(wrapper);
    }

    @Override
    public AppPageUtils coinRechargePage(Integer uid, Integer page, Integer limit) {
        Page<UserRechargeEntity> pageModel = new Page<>(page, limit);
        LambdaQueryWrapper<UserRechargeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRechargeEntity::getUid, uid)
                .eq(UserRechargeEntity::getType, Constant.RECHARGE_ORDER_COIN)
                .in(UserRechargeEntity::getStatus, Constant.RECHARGE_STATUS_PAID, Constant.RECHARGE_STATUS_REFUNDED)
                .orderByDesc(UserRechargeEntity::getId);
        IPage<UserRechargeEntity> result = this.page(pageModel, wrapper);
        return new AppPageUtils(result);
    }

    @Override
    @DSTransactional
    public void refundOrder(String orderId, BigDecimal refundAmount, String reason, String operatorName) {
        UserRechargeEntity order = this.lambdaQuery()
                .eq(UserRechargeEntity::getOrderId, orderId)
                .one();
        if (ObjectUtil.isNull(order)) {
            throw new LinfengException("订单不存在");
        }
        if (!Constant.RECHARGE_STATUS_PAID.equals(order.getStatus())) {
            throw new LinfengException("当前订单状态不允许退款");
        }
        BigDecimal finalRefundAmount = normalizeAmount(refundAmount);
        if (finalRefundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            finalRefundAmount = normalizeAmount(order.getPrice());
        }
        if (finalRefundAmount.compareTo(normalizeAmount(order.getPrice())) != 0) {
            throw new LinfengException("当前仅支持全额退款");
        }

        Integer orderType = order.getType() == null ? Constant.RECHARGE_ORDER_WALLET : order.getType();
        if (Constant.RECHARGE_ORDER_COIN.equals(orderType)) {
            rollbackCoinRecharge(order);
        } else if (Constant.RECHARGE_ORDER_VIP.equals(orderType)) {
            rollbackVipRecharge(order);
        } else {
            throw new LinfengException("当前订单类型暂不支持退款");
        }

        order.setStatus(Constant.RECHARGE_STATUS_REFUNDED);
        order.setRefundAmount(finalRefundAmount);
        order.setRemark(StrUtil.blankToDefault(reason, "后台退款"));
        order.setUpdateTime(DateUtil.nowDateTime());
        if (!this.updateById(order)) {
            throw new LinfengException("更新订单退款状态失败");
        }

        UserRechargeRefundEntity refund = new UserRechargeRefundEntity();
        refund.setRechargeId(order.getId());
        refund.setUid(order.getUid());
        refund.setOrderId(order.getOrderId());
        refund.setType(orderType);
        refund.setRefundNo("RF" + IdUtil.fastSimpleUUID().substring(0, 20));
        refund.setRefundAmount(finalRefundAmount);
        refund.setCoinAmount(resolveRefundCoinAmount(order));
        refund.setRefundStatus(1);
        refund.setReason(StrUtil.blankToDefault(reason, "后台退款"));
        refund.setOperatorName(StrUtil.blankToDefault(operatorName, "system"));
        refund.setTransactionId("mock_refund_" + System.currentTimeMillis());
        refund.setAddTime(DateUtil.nowDateTime());
        refund.setRefundTime(DateUtil.nowDateTime());
        userRechargeRefundDao.insert(refund);

        addOrderEventLog(order, Constant.BILL_EVENT_REFUND_SUCCESS, "退款成功");
    }

    @Override
    public List<UserRechargeRefundEntity> refundList(String orderId) {
        LambdaQueryWrapper<UserRechargeRefundEntity> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(orderId)) {
            wrapper.eq(UserRechargeRefundEntity::getOrderId, orderId);
        }
        wrapper.orderByDesc(UserRechargeRefundEntity::getId);
        return userRechargeRefundDao.selectList(wrapper);
    }

    private void rollbackCoinRecharge(UserRechargeEntity order) {
        int coinAmount = resolveCoinAmount(order);
        int currentIntegral = accountService.getCoinBalance(order.getUid()).intValue();
        if (currentIntegral < coinAmount) {
            throw new LinfengException("用户当前爱情币不足，无法退款");
        }
        BigDecimal remainIntegral = accountService.decreaseCoin(order.getUid(), coinAmount);
        org.aileme.common.redis.utils.RedisUtils.deleteObject("userId:" + order.getUid());
        billService.record(order.getUid(), 0, "爱情币退款扣回",
                BillDetailEnum.CATEGORY_2.getValue(), Constant.BILL_EVENT_COIN_REFUND,
                coinAmount, remainIntegral.doubleValue(), "后台退款扣回爱情币", order.getBizId(), order.getOrderId(), null, 1);
    }

    private void rollbackVipRecharge(UserRechargeEntity order) {
        if (StrUtil.isBlank(order.getBizId())) {
            throw new LinfengException("会员订单缺少套餐信息");
        }
        boolean hasLaterVipOrder = this.lambdaQuery()
                .eq(UserRechargeEntity::getUid, order.getUid())
                .eq(UserRechargeEntity::getType, Constant.RECHARGE_ORDER_VIP)
                .eq(UserRechargeEntity::getStatus, Constant.RECHARGE_STATUS_PAID)
                .gt(UserRechargeEntity::getId, order.getId())
                .count() > 0;
        if (hasLaterVipOrder) {
            throw new LinfengException("存在后续会员订单，暂不支持退款");
        }
        VipOptionEntity vipOption = vipOptionService.getById(Integer.valueOf(order.getBizId()));
        if (ObjectUtil.isNull(vipOption)) {
            throw new LinfengException("会员套餐不存在");
        }
        AppUserEntity user = userService.getById(order.getUid());
        Date expireTime = user.getVipExpireTime();
        if (ObjectUtil.isNull(expireTime)) {
            user.setVip(0);
            user.setVipExpireTime(null);
        } else {
            Date rollbackExpire = DateUtil.addDay(DateUtil.dateToStr(expireTime, "yyyy-MM-dd HH:mm:ss"), -vipOption.getValidDays());
            if (ObjectUtil.isNull(rollbackExpire) || rollbackExpire.before(new Date())) {
                user.setVip(0);
                user.setVipExpireTime(null);
            } else {
                user.setVip(Constant.VIP_USER);
                user.setVipExpireTime(rollbackExpire);
            }
        }
        if (!userService.saveOrUpdate(user)) {
            throw new LinfengException("回退会员失败");
        }
        org.aileme.common.redis.utils.RedisUtils.deleteObject("userId:" + order.getUid());
    }

    private int resolveCoinAmount(UserRechargeEntity order) {
        if (order.getCoinAmount() != null && order.getCoinAmount() > 0) {
            return order.getCoinAmount();
        }
        return calculateCoinAmount(order.getPrice(), order.getGivePrice());
    }

    private int resolveRefundCoinAmount(UserRechargeEntity order) {
        Integer orderType = order.getType() == null ? Constant.RECHARGE_ORDER_WALLET : order.getType();
        if (!Constant.RECHARGE_ORDER_COIN.equals(orderType)) {
            return 0;
        }
        return resolveCoinAmount(order);
    }

    private String buildActivityOrderTitle(XiangqinActivityEntity activity) {
        String title = activity == null ? "" : StrUtil.trim(activity.getTitle());
        if (StrUtil.isBlank(title)) {
            return "活动报名";
        }
        return "活动报名·" + title;
    }

    private String resolveActivityReturnPage(UserRechargeEntity order) {
        Integer activityId = resolveActivityIdFromOrder(order);
        if (activityId == null || activityId <= 0) {
            return "/pages/hongniang/index";
        }
        return "/pages/hongniang/activity-detail?id=" + activityId;
    }

    private Integer resolveActivityIdFromOrder(UserRechargeEntity order) {
        if (order == null) {
            return null;
        }
        if (StrUtil.isNotBlank(order.getRemark()) && NumberUtil.isInteger(order.getRemark())) {
            return Integer.valueOf(order.getRemark());
        }
        if (StrUtil.isBlank(order.getBizId()) || !NumberUtil.isInteger(order.getBizId())) {
            return null;
        }
        XiangqinEnrollmentEntity enrollment = xiangqinEnrollmentService.getById(Integer.valueOf(order.getBizId()));
        return enrollment == null ? null : enrollment.getActivityId();
    }

    private void confirmActivityEnrollment(UserRechargeEntity order) {
        if (order == null || StrUtil.isBlank(order.getBizId()) || !NumberUtil.isInteger(order.getBizId())) {
            throw new LinfengException("活动报名订单缺少报名记录");
        }
        Integer enrollmentId = Integer.valueOf(order.getBizId());
        xiangqinEnrollmentService.confirmPaidEnrollment(enrollmentId, order.getPrice(), order.getPayTime());

        XiangqinEnrollmentEntity enrollment = xiangqinEnrollmentService.getById(enrollmentId);
        XiangqinActivityEntity activity = enrollment == null ? null : xiangqinActivityService.getById(enrollment.getActivityId());
        AppUserEntity applicant = enrollment == null ? null : userService.getById(enrollment.getUserId());
        sendActivityEnrollmentNotice(activity, applicant, enrollment);
    }

    private void sendActivityEnrollmentNotice(XiangqinActivityEntity activity, AppUserEntity applicant, XiangqinEnrollmentEntity enrollment) {
        if (activity == null || applicant == null || enrollment == null) {
            return;
        }
        HongniangInfoEntity hongniang = activity.getHongniangId() == null ? null : hongniangService.getById(activity.getHongniangId());
        Integer organizerUid = hongniang == null ? null : hongniang.getUserId();
        if (organizerUid == null || organizerUid <= 0 || organizerUid.equals(applicant.getUid())) {
            return;
        }
        String applicantName = StrUtil.blankToDefault(StrUtil.trim(applicant.getUsername()), "有用户");
        String content = String.format("%s 报名了你的活动《%s》，请及时审核。", applicantName, activity.getTitle());
        if (StrUtil.isNotBlank(enrollment.getPhone())) {
            content = content + " 联系电话：" + enrollment.getPhone();
        }
        sendChatTextNotice(applicant.getUid(), organizerUid, content);
    }

    private void sendChatTextNotice(Integer senderUid, Integer receiverUid, String content) {
        if (senderUid == null || receiverUid == null || senderUid <= 0 || receiverUid <= 0 || senderUid.equals(receiverUid)) {
            return;
        }
        Long sessionId = friendService.getOrCreateSession(senderUid, receiverUid);
        ChatMessageEntity chatMessage = ChatMessageEntity.builder()
                .sessionId(String.valueOf(sessionId))
                .senderId(String.valueOf(senderUid))
                .receiverId(String.valueOf(receiverUid))
                .sendTime(DateUtil.nowDateTimeStr())
                .content(content)
                .messageType("text")
                .isWithdrawn(0)
                .updateTime(DateUtil.nowDateTime())
                .build();
        chatMessageService.saveMessage(chatMessage);
        SocketMessage<ChatMessageEntity> socketMessage = new SocketMessage<>(MessageConstant.PERSON_MESSAGE, chatMessage);
        SocketServer.sendToUser(String.valueOf(receiverUid), socketMessage);
        SocketServer.sendToUser(String.valueOf(senderUid), socketMessage);
    }

    private String resolveOrderTitle(Integer orderType) {
        if (Constant.RECHARGE_ORDER_VIP.equals(orderType)) {
            return "开通会员";
        }
        if (Constant.RECHARGE_ORDER_COIN.equals(orderType)) {
            return "购买爱情币";
        }
        if (Constant.RECHARGE_ORDER_ACTIVITY.equals(orderType)) {
            return "活动报名";
        }
        return "账户充值";
    }

    private void addOrderEventLog(UserRechargeEntity order, String eventType, String title) {
        payOrderDetailService.record(order, eventType, title, normalizeAmount(order.getPrice()),
                order.getCoinAmount(), buildOrderEventMark(order, title));
    }

    private String buildOrderEventMark(UserRechargeEntity order, String title) {
        List<String> parts = new ArrayList<>();
        parts.add(title);
        if (StrUtil.isNotBlank(order.getTitle())) {
            parts.add(order.getTitle());
        }
        if (normalizeAmount(order.getPrice()).compareTo(BigDecimal.ZERO) > 0) {
            parts.add("金额" + order.getPrice());
        }
        if (order.getCoinAmount() != null && order.getCoinAmount() > 0) {
            parts.add("到账" + order.getCoinAmount() + "爱情币");
        }
        return String.join("，", parts);
    }


}
