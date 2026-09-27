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
import cn.hutool.core.util.StrUtil;
import com.baomidou.dynamic.datasource.annotation.DS;

import cn.hutool.core.util.NumberUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.aileme.common.redis.utils.RedisUtils;
import org.aileme.shejiao.api.service.*;
import org.aileme.shejiao.common.enums.BillDetailEnum;
import org.aileme.shejiao.common.enums.BillEnum;
import org.aileme.shejiao.common.enums.BillInfoEnum;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.*;
import org.aileme.shejiao.admin.utils.WechatUtil;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.PostEntity;
import org.aileme.shejiao.domain.param.app.AddRewardForm;
import org.aileme.shejiao.domain.param.app.ExchangeForm;
import org.aileme.shejiao.domain.param.app.getBillListForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.aileme.shejiao.admin.dao.BillDao;
import org.aileme.shejiao.domain.entity.admin.BillEntity;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;


@DS("master")
@Service("billService")
public class BillServiceImpl extends ServiceImpl<BillDao, BillEntity> implements BillService {

    @Autowired
    private BillDao billDao;
    @Autowired
    private AppUserService userService;
    @Autowired
    private SysConfigService configService;
    @Autowired
    private PostService postService;
    @Autowired
    private MessageService messageService;
    @Autowired
    private UserLevelService userLevelService;
    @Autowired
    private AccountService accountService;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<BillEntity> queryWrapper=new QueryWrapper<>();

        //条件查询
        String uid = (String)params.get("key");
        String pm = (String)params.get("type");
        String category = (String)params.get("type2");
        if(!WechatUtil.isEmpty(category)){
            queryWrapper.lambda().eq(BillEntity::getCategory,category);
        }
        if(!WechatUtil.isEmpty(pm)){
            queryWrapper.lambda().eq(BillEntity::getPm,Integer.valueOf(pm));
        }

        if(NumberUtil.isInteger(uid)&&!WechatUtil.isEmpty(uid)){
            queryWrapper.lambda().eq(BillEntity::getUid,Integer.valueOf(uid));
        }
        queryWrapper.lambda().orderByDesc(BillEntity::getId);
        IPage<BillEntity> page = this.page(
                new Query<BillEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    /**
     * 增加支出流水
     *
     * @param uid      uid
     * @param title    账单标题
     * @param category 明细种类
     * @param type     明细类型
     * @param number   明细数字
     * @param balance  剩余
     * @param mark     备注
     * @param postUid  关联用户ID
     */
    public void expend(Integer uid, String title, String category, String type, double number, double balance, String mark, String linkId, Integer postUid){
        record(uid, BillEnum.PM_0.getValue(), title, category, type, number, balance, mark, linkId, null, postUid, 1);
    }

    /**
     * 增加收入/支入流水
     *
     * @param uid      uid
     * @param title    账单标题
     * @param category 明细种类
     * @param type     明细类型
     * @param number   明细数字
     * @param balance  剩余
     * @param mark     备注
     * @param linkid   关联id
     * @param tipUserId 关联用户ID
     */
    public void income(Integer uid, String title, String category, String type, double number,
                       double balance, String mark, String linkid, Integer tipUserId){
        record(uid, BillEnum.PM_1.getValue(), title, category, type, number, balance, mark, linkid, null, tipUserId, 1);
    }

    @Override
    public void record(Integer uid, Integer pm, String title, String category, String type, double number,
                       double balance, String mark, String linkId, String orderId, Integer tipUserId, Integer status) {
        BillEntity userBill = BillEntity.builder()
                .uid(uid)
                .title(title)
                .category(category)
                .type(type)
                .number(BigDecimal.valueOf(number))
                .balance(BigDecimal.valueOf(balance))
                .mark(mark)
                .pm(pm)
                .linkId(linkId)
                .orderId(orderId)
                .addTime(DateUtil.nowDateTime())
                .tipUserId(tipUserId)
                .status(status)
                .build();

        int insert = billDao.insert(userBill);
        if(insert!=1){
            throw new LinfengException("增加流水失败");
        }
    }

    /**
     * 判断是否支付
     * 用户自己看自己的付费贴不需要判断
     * @param postId
     * @param userId
     * @return
     */
    @Override
    public boolean vipPostIsPay(Integer postId, Integer userId) {
        BillEntity bill = this.lambdaQuery()
                .eq(BillEntity::getLinkId, postId)
                .eq(BillEntity::getUid, userId)
                .eq(BillEntity::getPm, BillEnum.PM_0.getValue())
                .eq(BillEntity::getType, BillDetailEnum.TYPE_3.getValue())
                .one();
        return bill!=null;
    }

    @Override
    public AppPageUtils billList(getBillListForm request, AppUserEntity user) {
        QueryWrapper<BillEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(BillEntity::getUid,user.getUid()).orderByDesc(BillEntity::getId);
        switch (BillInfoEnum.toType(request.getType())){
            case PAY:
                wrapper.lambda().eq(BillEntity::getCategory,BillDetailEnum.CATEGORY_1.getValue());
                wrapper.lambda().eq(BillEntity::getType,BillDetailEnum.TYPE_3.getValue());
                break;
            case RECHAREGE:
                wrapper.lambda().eq(BillEntity::getCategory,BillDetailEnum.CATEGORY_1.getValue());
                wrapper.lambda().eq(BillEntity::getType,BillDetailEnum.TYPE_1.getValue());
                break;
            case EXTRACT:
                wrapper.lambda().eq(BillEntity::getCategory,BillDetailEnum.CATEGORY_1.getValue());
                wrapper.lambda().eq(BillEntity::getType,BillDetailEnum.TYPE_4.getValue());
                break;
            case SIGN_INTEGRAL:
                wrapper.lambda().eq(BillEntity::getCategory,BillDetailEnum.CATEGORY_2.getValue());
                wrapper.lambda().eq(BillEntity::getType,BillDetailEnum.TYPE_10.getValue());
                break;
            default:
                wrapper.lambda().eq(BillEntity::getCategory,BillDetailEnum.CATEGORY_1.getValue());
        }
        Page<BillEntity> page = new Page<>(request.getPage(),request.getLimit());
        Page<BillEntity> pages = this.page(page, wrapper);
        return new AppPageUtils(pages);
    }

    @Override
    public BigDecimal getAllPay(Integer userId) {
        double sum=billDao.getAllPay(userId);
        return new BigDecimal(sum);
    }

    @Override
    public List<BillEntity> getIntegralList(Integer uid, Integer page, Integer limit,Integer type) {
        LambdaQueryWrapper<BillEntity> wrapper = new LambdaQueryWrapper<>();
        if(type==1){
            wrapper.eq(BillEntity::getPm,0)
                    .ne(BillEntity::getType, Constant.BILL_EVENT_COIN_REFUND);
        }else if (type==2){
            wrapper.eq(BillEntity::getPm,1);
        }
        wrapper.eq(BillEntity::getStatus, 1)
                .eq(BillEntity::getUid, uid)
                .eq(BillEntity::getCategory, BillDetailEnum.CATEGORY_2.getValue())
                .orderByDesc(BillEntity::getId);
        Page<BillEntity> pageModel = new Page<>(page, limit);
        IPage<BillEntity> pageList = billDao.selectPage(pageModel, wrapper);
        return pageList.getRecords();
    }

    /**
     * 获取用户消耗的积分
     * （目前消耗的唯一途径是兑换余额）
     * @param uid
     * @return
     */
    @Override
    public Integer getUsedIntegral(Integer uid) {

        return billDao.getUsedIntegral(uid);
    }

    @Override
    @DSTransactional
    public void exchange(AppUserEntity users, ExchangeForm request) {
        AppUserEntity user = userService.getById(users.getUid());
        BigDecimal amount = requirePositiveAmount(request);
        Integer integralRate = getIntegralRate();
        BigDecimal currentMoney = normalizeMoney(user);
        Integer currentIntegral = accountService.getCoinBalance(user.getUid()).intValue();
        BigDecimal requiredIntegral = amount.multiply(BigDecimal.valueOf(integralRate));
        if (requiredIntegral.stripTrailingZeros().scale() > 0) {
            throw new LinfengException("当前积分比例不支持该兑换金额");
        }
        int integralCost = requiredIntegral.intValueExact();
        if (integralCost > currentIntegral) {
            throw new LinfengException("兑换积分不足");
        }

        //更新积分和余额并删除缓存
        BigDecimal add = currentMoney.add(amount).setScale(2, RoundingMode.HALF_UP);
        Integer sub = currentIntegral - integralCost;
        boolean update = userService.lambdaUpdate()
                .set(AppUserEntity::getMoney, add)
                .eq(AppUserEntity::getUid, user.getUid())
                .update();
        if(!update){
            throw new LinfengException("用户信息更新失败");
        }
        accountService.decreaseCoin(user.getUid(), integralCost);
        org.aileme.common.redis.utils.RedisUtils.deleteObject("userId:" +user.getUid());
        //插入积分扣除的账单
        this.expend(user.getUid(),BillDetailEnum.TYPE_11.getDesc(),
                BillDetailEnum.CATEGORY_2.getValue(),BillDetailEnum.TYPE_11.getValue(),
                integralCost,currentIntegral,"积分兑换余额扣除积分","",null);
        //插入余额增加的账单
        this.income(user.getUid(),BillDetailEnum.TYPE_12.getDesc(),
                BillDetailEnum.CATEGORY_1.getValue(),BillDetailEnum.TYPE_12.getValue(),
                amount.doubleValue(),currentMoney.doubleValue(),"积分兑换余额增加余额","", user.getUid());
    }

    @Override
    @DSTransactional
    public void exchangeMoneyToIntegral(AppUserEntity users, ExchangeForm request) {
        AppUserEntity user = userService.getById(users.getUid());
        BigDecimal amount = requirePositiveAmount(request);
        BigDecimal currentMoney = normalizeMoney(user);
        Integer currentIntegral = accountService.getCoinBalance(user.getUid()).intValue();
        if (currentMoney.compareTo(amount) < 0) {
            throw new LinfengException("账户余额不足");
        }
        Integer integralRate = getIntegralRate();
        BigDecimal integralValue = amount.multiply(BigDecimal.valueOf(integralRate));
        if (integralValue.stripTrailingZeros().scale() > 0) {
            throw new LinfengException("当前积分比例不支持该兑换金额");
        }
        int addIntegral = integralValue.intValueExact();
        BigDecimal remainMoney = currentMoney.subtract(amount).setScale(2, RoundingMode.HALF_UP);
        Integer totalIntegral = currentIntegral + addIntegral;
        boolean update = userService.lambdaUpdate()
                .set(AppUserEntity::getMoney, remainMoney)
                .eq(AppUserEntity::getUid, user.getUid())
                .update();
        if (!update) {
            throw new LinfengException("用户信息更新失败");
        }
        accountService.increaseCoin(user.getUid(), addIntegral);
        RedisUtils.deleteObject("userId:" + user.getUid());
        this.expend(user.getUid(), BillDetailEnum.TYPE_17.getDesc(),
                BillDetailEnum.CATEGORY_1.getValue(), BillDetailEnum.TYPE_17.getValue(),
                amount.doubleValue(), currentMoney.doubleValue(), "余额兑换积分扣除余额", "", null);
        this.income(user.getUid(), BillDetailEnum.TYPE_18.getDesc(),
                BillDetailEnum.CATEGORY_2.getValue(), BillDetailEnum.TYPE_18.getValue(),
                addIntegral, currentIntegral, "余额兑换积分增加积分", "", user.getUid());
    }

    private Integer getIntegralRate() {
        String integral = configService.getValue(Constant.INTEGRAL);
        return Integer.valueOf(integral);
    }

    private BigDecimal requirePositiveAmount(ExchangeForm request) {
        BigDecimal amount = BigDecimal.valueOf(request.getRechargeValue()).setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new LinfengException("兑换金额必须大于0");
        }
        return amount;
    }

    private BigDecimal normalizeMoney(AppUserEntity user) {
        return user.getMoney() == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : user.getMoney().setScale(2, RoundingMode.HALF_UP);
    }

    private Integer normalizeIntegral(AppUserEntity user) {
        return accountService.getCoinBalance(user.getUid()).intValue();
    }

    @Override
    @DSTransactional
    public void rewardIntegral(AppUserEntity user, AddRewardForm request) {
        if(WechatUtil.isEmpty(request.getRewardCount())){
            throw new LinfengException("请输入打赏数量");
        }
        Integer rewardCount = request.getRewardCount();
        AppUserEntity appUser = userService.getById(user.getUid());
        int currentIntegral = accountService.getCoinBalance(user.getUid()).intValue();
        if (currentIntegral < rewardCount){
            throw new LinfengException("你的账户积分不足");
        }
        PostEntity post = postService.getById(request.getPostId());
        if(post.getUid().equals(user.getUid())){
            throw new LinfengException("不能打赏自己");
        }
        if(rewardCount>5000){
            throw new LinfengException("打赏数目过大,请重选");
        }
        if(rewardCount<=0){
            throw new LinfengException("打赏数目不合法");
        }

        BigDecimal commissionRate = parseCommissionRate(configService.getValue(Constant.REWARD_COMMISSION_RATE));
        int feeAmount = 0;
        int arrivalAmount = rewardCount;
        if (commissionRate.compareTo(BigDecimal.ZERO) > 0) {
            feeAmount = BigDecimal.valueOf(rewardCount)
                    .multiply(commissionRate)
                    .divide(new BigDecimal("100"), 0, RoundingMode.DOWN)
                    .intValue();
            if (feeAmount >= rewardCount) {
                feeAmount = rewardCount - 1;
            }
            arrivalAmount = rewardCount - feeAmount;
        }
        BigDecimal payerAfter = accountService.decreaseCoin(user.getUid(), rewardCount);
        AppUserEntity appUserEntity = userService.getById(post.getUid());
        BigDecimal receiverAfter = accountService.increaseCoin(post.getUid(), arrivalAmount);
        RedisUtils.deleteObject(RedisKeys.getUserCacheKey(post.getUid()));
        RedisUtils.deleteObject(RedisKeys.getUserCacheKey(user.getUid()));
        //检查等级LV
        userLevelService.checkUserLevel(user.getUid());
        userLevelService.checkUserLevel(post.getUid());
        //打赏用户 插入积分消耗的账单
        String payerMark = feeAmount > 0
                ? "打赏扣除积分（含平台服务费" + feeAmount + "积分）"
                : "打赏扣除积分";
        String receiverMark = feeAmount > 0
                ? "打赏增加积分（已扣除平台服务费" + feeAmount + "积分）"
                : "打赏增加积分";
        this.expend(user.getUid(),BillDetailEnum.TYPE_20.getDesc(),
            BillDetailEnum.CATEGORY_2.getValue(),BillDetailEnum.TYPE_20.getValue(),
            rewardCount,payerAfter.doubleValue(),payerMark,post.getId().toString(),post.getUid());
        //被打赏用户 插入积分增加的账单
        this.income(post.getUid(),BillDetailEnum.TYPE_19.getDesc(),
            BillDetailEnum.CATEGORY_2.getValue(),BillDetailEnum.TYPE_19.getValue(),
            arrivalAmount,receiverAfter.doubleValue(),receiverMark,post.getId().toString(),user.getUid());


        String content = StrUtil.format(Constant.REWARD_NOTICE,user.getUsername(),post.getTitle(),arrivalAmount);
        messageService.sendMessageNotAsync(0,post.getUid(),post.getId(),Constant.PUSHARTICLE,content,Constant.TITLE_REWARD);
    }

    @Override
    public List<Map<String, Object>> getPostRewardList(Integer postId) {
        // 查询该帖子的打赏记录（支出类型）
        LambdaQueryWrapper<BillEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BillEntity::getType, BillDetailEnum.TYPE_20.getValue())
               .eq(BillEntity::getLinkId, String.valueOf(postId))
               .eq(BillEntity::getStatus, 1)
               .orderByDesc(BillEntity::getAddTime)
               .last("limit 10");
        List<BillEntity> list = this.list(wrapper);

        // 转换为返回格式
        return list.stream().map(bill -> {
            Map<String, Object> map = new java.util.HashMap<>();
            AppUserEntity user = userService.getById(bill.getUid());
            map.put("uid", bill.getUid());
            map.put("avatar", user != null ? user.getAvatar() : "");
            map.put("username", user != null ? user.getUsername() : "");
            map.put("rewardCount", bill.getNumber().intValue());
            map.put("createTime", bill.getAddTime());
            return map;
        }).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public Integer getPostRewardTotal(Integer postId) {
        // 查询该帖子的打赏总积分
        LambdaQueryWrapper<BillEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BillEntity::getType, BillDetailEnum.TYPE_20.getValue())
               .eq(BillEntity::getLinkId, String.valueOf(postId))
               .eq(BillEntity::getStatus, 1);
        List<BillEntity> list = this.list(wrapper);

        return list.stream()
                   .mapToInt(bill -> bill.getNumber().intValue())
                   .sum();
    }

    private BigDecimal parseCommissionRate(String configValue) {
        if (StrUtil.isBlank(configValue)) {
            return BigDecimal.ZERO;
        }
        try {
            BigDecimal rate = new BigDecimal(configValue.trim());
            if (rate.compareTo(BigDecimal.ZERO) < 0) {
                return BigDecimal.ZERO;
            }
            if (rate.compareTo(new BigDecimal("100")) > 0) {
                return new BigDecimal("100");
            }
            return rate;
        } catch (Exception ignored) {
            return BigDecimal.ZERO;
        }
    }

}
