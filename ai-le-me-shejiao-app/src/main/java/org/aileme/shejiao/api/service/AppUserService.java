package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import me.chanjar.weixin.mp.bean.result.WxMpUser;
import org.aileme.shejiao.domain.vo.AppHotUserResponse;
import org.aileme.shejiao.domain.vo.AppUserInfoResponse;
import org.aileme.shejiao.domain.vo.AppUserResponse;
import org.aileme.shejiao.domain.vo.HomeRateResponse;
import org.aileme.shejiao.common.utils.AppPageUtils;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.MessageEntity;
import org.aileme.shejiao.domain.param.app.*;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 *
 *
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-20 12:10:43
 */
public interface AppUserService extends IService<AppUserEntity> {

    PageUtils queryPage(Map<String, Object> params);

    AppPageUtils findTopicUserPage(TopicUserForm form,AppUserEntity user);

    AppUserResponse getUserInfo(AppUserEntity user);

    Integer wxLogin(WxLoginForm form,HttpServletRequest request);

    void updateAppUserInfo(AppUserUpdateForm appUserUpdateForm, AppUserEntity user);

    void addFollow(AddFollowForm request, AppUserEntity user);

    void cancelFollow(AddFollowForm request, AppUserEntity user);

    AppUserInfoResponse findUserInfoById(Integer uid, AppUserEntity user);

    AppPageUtils search(Integer page, String keyword,Integer uid);

    AppPageUtils userFans(Integer page,Integer target, Integer uid);

    AppPageUtils follow(Integer page,Integer uid, AppUserEntity user);

    void ban(Integer id);

    void openBan(Integer id);

    String sendSmsCode(SendCodeForm request);

    String sendEmailCode(SendCodeForm request);

    Integer smsLogin(SmsLoginForm form, HttpServletRequest request);

    Integer register(SmsLoginForm form, HttpServletRequest request);

    Integer getTotalNum();

    /**
     * 根据日期获取注册用户数量
     * @param date 日期
     * @return Integer
     */
    Integer getRegisterNumByDate(String date);

    /**
     * 首页数据
     * @return HomeRateResponse
     */
    HomeRateResponse indexDate();
    /**
     * 本月新增用户
     * @return map
     */
    Map<String,Object> chartCount();

    BigDecimal updateMoney(Integer uid, BigDecimal price, BigDecimal givePrice);

    void updateUser(AdminUserInfoForm user);

    List<AppUserEntity> getBatchUser(List<Integer> uid);

    List<MessageEntity> systemInfoList(ChatListForm request, AppUserEntity user);

    AppUserEntity vipExpirationCheck(AppUserEntity user);

    List<AppHotUserResponse> getHotUserList();

    Integer bindWxPhone(LoginPhoneParam param,HttpServletRequest request);

    Map<String,Object>  chartPost();

    Map<String,Object>  chartMoney();

    void punishUser(AdminUserPunishForm param);

    AppUserEntity saveMpWxUser(WxMpUser wxMpUser);

    void cancelAccount(AppUserEntity user, String reason);
}
