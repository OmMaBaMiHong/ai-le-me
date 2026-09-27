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

import com.google.common.collect.Maps;
import org.aileme.shejiao.domain.param.app.*;
import org.aileme.shejiao.domain.vo.OrderPay;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.*;
import org.aileme.shejiao.app.utils.WechatUtil;
import org.aileme.shejiao.common.validator.ValidatorUtils;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.RechargeEntity;
import org.aileme.shejiao.api.service.AccountService;
import org.aileme.shejiao.api.service.BillService;
import org.aileme.shejiao.api.service.PaymentRuntimeConfigService;
import org.aileme.shejiao.api.service.RechargeService;
import org.aileme.shejiao.api.service.UserRechargeService;
import org.aileme.shejiao.app.annotation.Login;
import org.aileme.shejiao.app.annotation.LoginUser;
import org.aileme.shejiao.app.runtime.compliance.MiniAppFilingFeatureService;
import org.aileme.shejiao.common.utils.weixin.sdk.HttpKit;
import org.aileme.shejiao.common.utils.weixin.sdk.PaymentKit;
import org.aileme.shejiao.common.utils.weixin.sdk.WXPayUtil;
import org.aileme.shejiao.api.service.SysConfigService;
import org.aileme.shejiao.api.service.WechatRuntimeConfigService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Parameter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.Map;


/**
 * 用户充值
 *
 * @author linfeng
 * @email linfengtech001@163.com
 * @date 2022-04-19 19:27:33
 */
@Tag(name = "移动端——用户充值")
@RestController
@RequestMapping("app")
@Slf4j
public class AppUserRechargeController {

    @Autowired
    private UserRechargeService userRechargeService;
    @Autowired
    private BillService billService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private RechargeService rechargeService;
    @Autowired
    private SysConfigService configService;

    @Autowired
    private WechatRuntimeConfigService wechatRuntimeConfigService;

    @Autowired
    private MiniAppFilingFeatureService miniAppFilingFeatureService;
    @Autowired
    private PaymentRuntimeConfigService paymentRuntimeConfigService;



    /**
     * 付费贴支付
     * @param param
     * @param user
     * @return
     */
    @Login
    @PostMapping("/user/payVipPost")
    @Operation(summary = "付费贴支付")
    public Result payVipPost(@RequestBody PayVipPostForm param,@Parameter(hidden = true) @LoginUser AppUserEntity user){
        miniAppFilingFeatureService.requirePaymentEnabled();
        userRechargeService.payVipPost(param,user);
        return new Result<>();
    }
    /**
     * 会员充值
     * APP微信支付
     * @return
     */
    @Login
    @PostMapping("/user/rechargeByApp")
    @Operation(summary = "App会员充值微信支付")
    public R rechargeByApp(@RequestBody RechargeForm param,@Parameter(hidden = true) @LoginUser AppUserEntity user, HttpServletRequest request) throws Exception {
        miniAppFilingFeatureService.requirePaymentEnabled();
        ValidatorUtils.validateEntity(param);

        String orderSn = userRechargeService.addRecharge(user,param.getPrice().toString(),param.getPaidPrice().toString(),Constant.PAY_TYPE_APP);
        String ip= AppletPayUtil.getClientIp(request);
        OrderPay orderPay=userRechargeService.goToPay(orderSn,param.getPrice(),ip,user,Constant.PAY_TYPE_APP);
        return R.ok().put("data",orderPay);
    }
    /**
     * 会员充值
     * 微信小程序支付
     * @return
     */
    @Login
    @PostMapping("/user/recharge")
    @Operation(summary = "会员充值微信小程序支付")
    public R recharge(@RequestBody RechargeForm param, @Parameter(hidden = true) @LoginUser AppUserEntity user, HttpServletRequest request) throws Exception {
        miniAppFilingFeatureService.requirePaymentEnabled();
        ValidatorUtils.validateEntity(param);

        String orderSn = userRechargeService.addRecharge(user,param.getPrice().toString(),param.getPaidPrice().toString(),Constant.PAY_TYPE_WX);
        String ip= AppletPayUtil.getClientIp(request);
        OrderPay orderPay=userRechargeService.goToPay(orderSn,param.getPrice(),ip,user,Constant.PAY_TYPE_WX);
        return R.ok().put("data",orderPay);
    }

    /**
     * 爱情币充值
     * 微信小程序支付
     */
    @Login
    @PostMapping("/user/rechargeCoin")
    @Operation(summary = "爱情币充值微信小程序支付")
    public R rechargeCoin(@RequestBody RechargeForm param, @Parameter(hidden = true) @LoginUser AppUserEntity user, HttpServletRequest request) throws Exception {
        miniAppFilingFeatureService.requirePaymentEnabled();
        ValidatorUtils.validateEntity(param);
        RechargeEntity product = getRechargeProduct(param.getRecharId());
        String orderSn = userRechargeService.addRecharge(user, product, Constant.PAY_TYPE_WX, param.getPayChannel(), Constant.RECHARGE_ORDER_COIN);
        String ip= AppletPayUtil.getClientIp(request);
        OrderPay orderPay=userRechargeService.goToPay(orderSn,product.getPrice().doubleValue(),ip,user,Constant.PAY_TYPE_WX);
        return R.ok().put("data",orderPay);
    }
    /**
     * 会员充值
     * H5支付
     * @return
     */
    @Login
    @PostMapping("/user/rechargeByH5")
    @Operation(summary = "会员充值H5支付")
    public R rechargeByH5(@RequestBody RechargeForm param,@Parameter(hidden = true)  @LoginUser AppUserEntity user, HttpServletRequest request) throws Exception {
        miniAppFilingFeatureService.requirePaymentEnabled();
        ValidatorUtils.validateEntity(param);

        String orderSn = userRechargeService.addRecharge(user,param.getPrice().toString(),param.getPaidPrice().toString(),Constant.PAY_TYPE_H5);
        String ip= AppletPayUtil.getClientIp(request);
        OrderPay orderPay=userRechargeService.goToPay(orderSn,param.getPrice(),ip,user,Constant.PAY_TYPE_H5,
                param.getPayChannel(), resolveH5Origin(request));
        return R.ok().put("data",orderPay);
    }

    /**
     * 爱情币充值
     * H5支付
     */
    @Login
    @PostMapping("/user/rechargeCoinByH5")
    @Operation(summary = "爱情币充值H5支付")
    public R rechargeCoinByH5(@RequestBody RechargeForm param,@Parameter(hidden = true)  @LoginUser AppUserEntity user, HttpServletRequest request) throws Exception {
        miniAppFilingFeatureService.requirePaymentEnabled();
        ValidatorUtils.validateEntity(param);
        RechargeEntity product = getRechargeProduct(param.getRecharId());
        String orderSn = userRechargeService.addRecharge(user, product, Constant.PAY_TYPE_H5, param.getPayChannel(), Constant.RECHARGE_ORDER_COIN);
        String ip= AppletPayUtil.getClientIp(request);
        OrderPay orderPay=userRechargeService.goToPay(orderSn,product.getPrice().doubleValue(),ip,user,Constant.PAY_TYPE_H5,
                param.getPayChannel(), resolveH5Origin(request));
        return R.ok().put("data",orderPay);
    }

    /**
     * 爱情币充值
     * APP微信支付
     */
    @Login
    @PostMapping("/user/rechargeCoinByApp")
    @Operation(summary = "爱情币充值App微信支付")
    public R rechargeCoinByApp(@RequestBody RechargeForm param,@Parameter(hidden = true) @LoginUser AppUserEntity user, HttpServletRequest request) throws Exception {
        miniAppFilingFeatureService.requirePaymentEnabled();
        ValidatorUtils.validateEntity(param);
        RechargeEntity product = getRechargeProduct(param.getRecharId());
        String orderSn = userRechargeService.addRecharge(user, product, Constant.PAY_TYPE_APP, param.getPayChannel(), Constant.RECHARGE_ORDER_COIN);
        String ip= AppletPayUtil.getClientIp(request);
        OrderPay orderPay=userRechargeService.goToPay(orderSn,product.getPrice().doubleValue(),ip,user,Constant.PAY_TYPE_APP);
        return R.ok().put("data",orderPay);
    }


    /**
     * 微信支付回调
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @PostMapping("/pay/rolBack")
    @Operation(summary = "微信支付回调")
    public void wxPayNotify(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("进入微信支付回调");
        String xmlMsg = HttpKit.readData(request);
        log.info("微信通知信息"+xmlMsg);
        Map<String, String> resultMap = PaymentKit.xmlToMap(xmlMsg);
        if (WechatUtil.isEmpty(resultMap)) {
            log.warn("微信支付回调解析失败，resultMap为空");
            writeWxPayResponse(response, false, "xml parse fail");
            return;
        }

        String returnCode = resultMap.get(Constant.WX_RETURN_CODE);
        String resultCode = resultMap.get("result_code");
        if (!Constant.WX_PAY_SUCCESS.equals(returnCode) || !Constant.WX_PAY_SUCCESS.equals(resultCode)) {
            log.warn("微信支付回调未成功 return_code={}, result_code={}, data={}", returnCode, resultCode, resultMap);
            writeWxPayResponse(response, false, "pay not success");
            return;
        }

        String key = wechatRuntimeConfigService.getWechatPayApiKey();
        if (WechatUtil.isEmpty(key)) {
            log.error("微信支付回调验签失败，商户密钥未配置");
            writeWxPayResponse(response, false, "pay key missing");
            return;
        }

        if (!WXPayUtil.isSignatureValid(resultMap, key)) {
            log.warn("微信支付回调验签失败: {}", resultMap);
            writeWxPayResponse(response, false, "invalid sign");
            return;
        }

        String orderNo = resultMap.get("out_trade_no");
        if (WechatUtil.isEmpty(orderNo)) {
            log.warn("微信支付回调缺少 out_trade_no");
            writeWxPayResponse(response, false, "out_trade_no missing");
            return;
        }

        String[] split = orderNo.split("-");
        if (split.length == 0 || WechatUtil.isEmpty(split[0])) {
            log.warn("微信支付回调订单号格式异常 out_trade_no={}", orderNo);
            writeWxPayResponse(response, false, "invalid out_trade_no");
            return;
        }

        try {
            String orderId = split[0];
            log.info("处理支付成功后的业务逻辑 orderId={}, outTradeNo={}", orderId, orderNo);
            userRechargeService.handleWxLog(orderId, resultMap.get("transaction_id"), orderNo);
            writeWxPayResponse(response, true, "OK");
        } catch (Exception e) {
            log.error("微信支付回调业务处理失败 orderNo={}", orderNo, e);
            writeWxPayResponse(response, false, "biz fail");
        }
    }

    @PostMapping("/pay/alipay/notify")
    @Operation(summary = "支付宝支付回调")
    public String alipayNotify(HttpServletRequest request) {
        Map<String, String> params = flattenRequestParams(request);
        if (WechatUtil.isEmpty(params)) {
            return "failure";
        }
        String sign = params.get("sign");
        String alipayPublicKey = paymentRuntimeConfigService.getAlipayPublicKey();
        if (WechatUtil.isEmpty(sign) || WechatUtil.isEmpty(alipayPublicKey)) {
            return "failure";
        }
        boolean verified = AlipaySignUtils.verify(params, alipayPublicKey, sign);
        if (!verified) {
            log.warn("支付宝回调验签失败: {}", params);
            return "failure";
        }
        String tradeStatus = params.get("trade_status");
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            String orderId = params.get("out_trade_no");
            String tradeNo = params.get("trade_no");
            if (!WechatUtil.isEmpty(orderId) && !WechatUtil.isEmpty(tradeNo)) {
                userRechargeService.handleWxLog(orderId, tradeNo, orderId);
            }
        }
        return "success";
    }

    @Login
    @PostMapping("/pay/mock/confirm")
    @Operation(summary = "mock支付确认")
    public R mockPayConfirm(@RequestBody MockPayConfirmForm param, @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        if (WechatUtil.isEmpty(param.getOrderId())) {
            return R.error("订单号不能为空");
        }
        String status = WechatUtil.isEmpty(param.getStatus()) ? Constant.PAY_MOCK_STATUS_SUCCESS : param.getStatus();
        userRechargeService.handleMockPay(param.getOrderId(), param.getPayChannel(), status);
        return R.ok().put("status", status).put("uid", user.getUid());
    }

    /**
     * 用户账单分页查询
     * @param request
     * @param user
     * @return
     */
    @Login
    @PostMapping("/bill/billList")
    @Operation(summary = "用户账单分页查询")
    public Result<AppPageUtils> billList(@RequestBody getBillListForm request,@Parameter(hidden = true) @LoginUser AppUserEntity user){
        AppPageUtils page=billService.billList(request,user);
        return new Result<AppPageUtils>().ok(page);
    }

    @Login
    @GetMapping("/user/rechargeRecord")
    @Operation(summary = "当前用户爱情币充值记录")
    public Result<AppPageUtils> rechargeRecord(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                               @RequestParam(value = "limit", defaultValue = "10") Integer limit,
                                               @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        miniAppFilingFeatureService.requirePaymentEnabled();
        AppPageUtils data = userRechargeService.coinRechargePage(user.getUid(), page, limit);
        return new Result<AppPageUtils>().ok(data);
    }

    @Login
    @PostMapping("/user/bill")
    @Operation(summary = "用户账户详情")
    public Result<Object> userBill(@Parameter(hidden = true) @LoginUser AppUserEntity user){
        miniAppFilingFeatureService.requirePaymentEnabled();
        String isOpen = configService.getValue(Constant.CHARGE);
        String exchange = configService.getValue(Constant.EXCHANGE);
        String integral = configService.getValue(Constant.INTEGRAL);
        String canCash = configService.getValue(Constant.CAN_CASH_OUT);
        Map<String,Object> map = Maps.newHashMap();
        map.put("nowMoney",user.getMoney() == null ? BigDecimal.ZERO.setScale(2) : user.getMoney());
        map.put("orderStatusSum",billService.getAllPay(user.getUid()));
        map.put("isHide",isOpen);
        map.put("exchange",exchange);
        map.put("integral",integral);
        map.put("canCash",canCash);
        map.put("allIntegral",accountService.getCoinBalance(user.getUid()).intValue());
        map.put("consumer",billService.getUsedIntegral(user.getUid()));

        return new Result<>().ok(map);
    }


    /**
     * 积分兑换余额
     * @param user
     * @return
     */
    @Login
    @PostMapping("/bill/exchange")
    @Operation(summary = "积分兑换余额")
    public Result exchange(@Parameter(hidden = true) @LoginUser AppUserEntity user,
                           @RequestBody ExchangeForm request){
        miniAppFilingFeatureService.requirePaymentEnabled();
        billService.exchange(user,request);
        return new Result();
    }

    /**
     * 余额兑换积分
     * @param user
     * @return
     */
    @Login
    @PostMapping("/bill/exchangeToIntegral")
    @Operation(summary = "余额兑换积分")
    public Result exchangeToIntegral(@Parameter(hidden = true) @LoginUser AppUserEntity user,
                                     @RequestBody ExchangeForm request){
        miniAppFilingFeatureService.requirePaymentEnabled();
        billService.exchangeMoneyToIntegral(user, request);
        return new Result();
    }

    /**
     * 回调后业务逻辑测试
     * @return
     */
    @Login
    @PostMapping("/pay/test")
    @Operation(summary = "测试回调后业务逻辑")
    public Result billList(){
        userRechargeService.handleWxLog("1521112168802549760","777testorder999","90payorderId787878");
        return new Result();
    }

    @Login
    @PostMapping("/bill/reward")
    @Operation(summary = "打赏积分")
    public Result reward(@Parameter @LoginUser AppUserEntity user,
                         @RequestBody AddRewardForm request){
        miniAppFilingFeatureService.requirePaymentEnabled();
        billService.rewardIntegral(user,request);
        return new Result();
    }

    /**
     * 获取帖子打赏列表
     * @param postId 帖子ID
     * @return 打赏用户列表和总积分
     */
    @GetMapping("/bill/rewardList")
    @Operation(summary = "获取帖子打赏列表")
    public Result<Map<String, Object>> getPostRewardList(@RequestParam Integer postId){
        miniAppFilingFeatureService.requirePaymentEnabled();
        Map<String, Object> result = Maps.newHashMap();
        result.put("rewardList", billService.getPostRewardList(postId));
        result.put("rewardTotal", billService.getPostRewardTotal(postId));
        return new Result<Map<String, Object>>().ok(result);
    }

    private RechargeEntity getRechargeProduct(String rechargeId) {
        if (WechatUtil.isEmpty(rechargeId)) {
            throw new LinfengException("充值商品不能为空");
        }
        RechargeEntity product = rechargeService.getById(Integer.valueOf(rechargeId));
        if (product == null || product.getStatus() == null || product.getStatus() != 0) {
            throw new LinfengException("充值商品不存在或已下架");
        }
        if (!Constant.PAY_PRODUCT_TYPE_COIN.equals(product.getProductType())) {
            throw new LinfengException("当前商品不是爱情币充值方案");
        }
        return product;
    }

    private String resolveH5Origin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (WechatUtil.isEmpty(origin)) {
            String referer = request.getHeader("Referer");
            if (!WechatUtil.isEmpty(referer)) {
                int index = referer.indexOf("/#");
                origin = index > 0 ? referer.substring(0, index) : referer;
            }
        }
        if (!WechatUtil.isEmpty(origin) && origin.endsWith("/")) {
            origin = origin.substring(0, origin.length() - 1);
        }
        return origin;
    }

    private Map<String, String> flattenRequestParams(HttpServletRequest request) {
        Map<String, String> result = Maps.newHashMap();
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (parameterMap == null || parameterMap.isEmpty()) {
            return result;
        }
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String[] values = entry.getValue();
            if (values == null || values.length == 0) {
                result.put(entry.getKey(), null);
            } else if (values.length == 1) {
                result.put(entry.getKey(), values[0]);
            } else {
                result.put(entry.getKey(), String.join(",", values));
            }
        }
        return result;
    }

    private void writeWxPayResponse(HttpServletResponse response, boolean success, String message) throws Exception {
        String result = success
                ? "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[" + message + "]]></return_msg></xml>"
                : "<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[" + message + "]]></return_msg></xml>";
        response.getWriter().write(result);
    }


}
