package org.aileme.shejiao.app.controller;

import cn.hutool.core.bean.BeanUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.util.RandomUtils;
import me.chanjar.weixin.mp.api.WxMpMessageRouter;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.config.WxMpConfigStorage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import me.chanjar.weixin.mp.bean.result.WxMpQrCodeTicket;
import org.aileme.common.redis.utils.RedisUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.common.utils.Result;
import org.aileme.shejiao.domain.vo.WxMpQrVO;
import org.aileme.shejiao.app.biz.WeChatBiz;

import jakarta.servlet.http.HttpServletRequest;

import static org.aileme.shejiao.common.utils.RedisKeys.WX_LOGIN_CODE_;

/**
 * @author Mr.Peng
 */
@Tag(name = "移动端——微信公众号授权")
@Slf4j
@RequestMapping("/app/wechat")
@RestController
public class WechatLoginController {

    @Autowired
    private WeChatBiz weChatBiz;
    @Autowired
    private WxMpService wxMpService;
    @Autowired
    private WxMpMessageRouter messageRouter;
    /**
     * 校验微信token
     */
    @GetMapping("/callback")
    @Operation(summary = "校验微信token", hidden = true)
    public String checkSignature(@RequestParam String signature,
                                 @RequestParam String timestamp,
                                 @RequestParam String nonce,
                                 @RequestParam String echostr) {
        if (!hasUsableWechatConfig()) {
            log.warn("微信公众号回调校验被跳过，当前未完成 wechat_mp 三方配置");
            return "";
        }
        // 通过检验signature对请求进行校验，若校验成功则原样返回echostr，表示接入成功，否则接入失败
        log.info("校验微信token,start");
        if (wxMpService.checkSignature(timestamp, nonce, signature)) {
            return echostr;
        }
        return "";
    }
    /**
     * 接收推送的数据
     */
    @PostMapping("/callback")
    @Operation(summary = "接收微信推送的数据", hidden = true,description = "按事件路由处理")
    public String wechat(HttpServletRequest request, @RequestBody String requestBody) {
        if (!hasUsableWechatConfig()) {
            throw new LinfengException("微信公众号能力未配置完成，请先补齐 wechat_mp 三方配置");
        }
        WxMpXmlMessage mpXmlMessage=WxMpXmlMessage.fromXml(requestBody);
        //根据路由规则处理
        WxMpXmlOutMessage wxMpXmlOutMessage= messageRouter.route(mpXmlMessage);
        return wxMpXmlOutMessage == null ? "" : wxMpXmlOutMessage.toXml();
    }
    /**
     * 获取微信公众号二维码链接
     **/
    @GetMapping("/getMpQrCode")
    @Operation(summary = "获取公众号二维码图片", description = "注意有效期和sceneId")
    public Result<WxMpQrVO> getMpQrCode() {
        ensureWechatConfigured();
        //sceneId 场景值 是我自定义的随机唯一字符串，登录使用，也可写死
        String sceneId = RandomUtils.getRandomStr();
        WxMpQrCodeTicket wxMpQrCodeTicket;
        try {
            wxMpQrCodeTicket = wxMpService.getQrcodeService().qrCodeCreateTmpTicket(sceneId, 600);
            //300为二维码有效期，单位秒
        } catch (Exception e) {
            log.error("获取微信公众号二维码链接失败", e);
            throw buildQrCodeException(e);
        }
        WxMpQrVO wxMpQrVO = new WxMpQrVO();
        BeanUtil.copyProperties(wxMpQrCodeTicket, wxMpQrVO);
        wxMpQrVO.setSceneId(sceneId);
        return new Result<WxMpQrVO>().ok(wxMpQrVO);
    }
    @GetMapping("/checkLogin")
    @Operation(summary = "二维码扫描登陆检查", description = "注意有效期和sceneId")
    @ResponseBody
    public R checRkLogin(@RequestParam String sceneId)
    {
        if (StringUtils.isBlank(sceneId)) {
            return R.error("sceneId不能为空");
        }
        String token= RedisUtils.getCacheObject(WX_LOGIN_CODE_+sceneId);
        if(StringUtils.isNotBlank(token)){
            return R.ok().put("token",token);
        }
        return R.error(202, "等待扫码");
    }

    private LinfengException buildQrCodeException(Exception e) {
        String message = e.getMessage();
        if (StringUtils.containsIgnoreCase(message, "invalid appid")
                || StringUtils.contains(message, "40013")
                || StringUtils.contains(message, "不合法的 AppID")) {
            return new LinfengException("微信公众号配置无效，请检查 wechat_mp 三方配置");
        }
        return new LinfengException("获取微信公众号二维码链接失败");
    }

    private void ensureWechatConfigured() {
        if (!hasUsableWechatConfig()) {
            throw new LinfengException("微信公众号能力未配置完成，请先补齐 wechat_mp 三方配置");
        }
    }

    private boolean hasUsableWechatConfig() {
        WxMpConfigStorage configStorage = wxMpService.getWxMpConfigStorage();
        String appId = configStorage == null ? null : configStorage.getAppId();
        String secret = configStorage == null ? null : configStorage.getSecret();
        return StringUtils.isNotBlank(appId)
                && StringUtils.isNotBlank(secret);
    }
    //    @ApiOperation("微信公众号服务器配置校验token")
//    @RequestMapping(value = "/wechat",method = RequestMethod.GET)
//    public void wechaCheckToken(HttpServletRequest request, HttpServletResponse response) {
//        //token验证代码段
//        try {
//            log.info("请求已到达，开始校验token");
//            if (StringUtils.isNotBlank(request.getParameter("signature"))) {
//                String signature = request.getParameter("signature");
//                String timestamp = request.getParameter("timestamp");
//                String nonce = request.getParameter("nonce");
//                String echostr = request.getParameter("echostr");
//                log.info("signature[{}], timestamp[{}], nonce[{}], echostr[{}]", signature, timestamp, nonce, echostr);
//                if (WeChatUtil.checkSignature(signature, timestamp, nonce)) {
//                    log.info("数据源为微信后台，将echostr[{}]返回！", echostr);
//                    BufferedOutputStream out = new BufferedOutputStream(response.getOutputStream());
//                    out.write(echostr.getBytes());
//                    out.flush();
//                    out.close();
//                }
//            }
//        } catch (Exception e) {
//            log.error("校验出错");
//            e.printStackTrace();
//        }
//    }
//    @ApiOperation("处理微信服务器的消息转发")
//    @PostMapping(value = "wechat")
//    public String  wechat(HttpServletRequest request) throws Exception {
//        // 调用parseXml方法解析请求消息
//        Map<String,String> requestMap = WeChatUtil.parseXml(request);
//        // 消息类型
//        String msgType = requestMap.get("MsgType");
//        // xml格式的消息数据
//        String respXml = null;
//        String mes = requestMap.get("Content");
//        // 文本消息
//        if ("text".equals(msgType) && "验证码".equals(mes)) {
//            String code = RandomUtil.randomNumbers(6);
//            respXml=WeChatUtil.sendTextMsg(requestMap,code);
//          //  redisCache.setCacheObject(RedisConstants.WECHAT_CODE+code,code,30, TimeUnit.MINUTES);
//        }
//        return respXml;
//    }
//
//    @ApiOperation(value = "发送验证码", hidden = true)
//    @ResponseBody
//    @RequestMapping(value = "/sendVertficationCode", produces = { "application/json;charset=utf-8" })
//    public String sendVertficationCode(HttpServletRequest request, @RequestParam(required = true) String echostr,
//                                       @RequestParam String userId) {
////      userId = o3FqD1sJQdv0oQz_dEPvbgk3AFbE;
//        weChatBiz.returnVerficationCode(userId);
//        return echostr;
//    }



}
