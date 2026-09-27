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

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import org.aileme.shejiao.common.enums.BillDetailEnum;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.*;
import org.aileme.shejiao.admin.utils.WechatUtil;import org.aileme.shejiao.domain.vo.AppCashInfoResponse;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.api.service.BillService;
import org.aileme.shejiao.api.service.MessageService;
import org.aileme.shejiao.api.service.SysConfigService;
import org.aileme.shejiao.domain.param.app.AddCashOutForm;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.aileme.shejiao.admin.dao.CashOutDao;
import org.aileme.shejiao.domain.entity.admin.CashOutEntity;
import org.aileme.shejiao.api.service.CashOutService;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;


@DS("master")
@Service("cashOutService")
public class CashOutServiceImpl extends ServiceImpl<CashOutDao, CashOutEntity> implements CashOutService {

    @Autowired
    private AppUserService userService;
    @Autowired
    private SysConfigService configService;
    @Autowired
    private MessageService messageService;
    @Autowired
    private BillService billService;



    @Override
    public PageUtils queryPage(Map<String, Object> params) {

        QueryWrapper<CashOutEntity> queryWrapper=new QueryWrapper<>();
        //条件查询
        String key = (String)params.get("key");
        String type = (String)params.get("type");
        if(NumberUtil.isInteger(key)){
            if(!WechatUtil.isEmpty(key)){
                queryWrapper.lambda().eq(CashOutEntity::getUid,Integer.parseInt(key));
            }
        }
        if(!WechatUtil.isEmpty(type)){
            queryWrapper.lambda().eq(CashOutEntity::getStatus, Integer.parseInt(type));
        }
        queryWrapper.lambda().orderByDesc(CashOutEntity::getId);
        IPage<CashOutEntity> page = this.page(
                new Query<CashOutEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    /**
     * 用户提交提现申请
     * @param param
     * @param uid
     */
    @Override
    @DSTransactional
    public void submit(AddCashOutForm param,Integer uid) {
        if(!checkIsNormal(uid)){
            throw new LinfengException("存在待审核提现申请");
        }
        checkAccountMoney(uid,param.getMoneyNumber());
        CashOutEntity cashOut=new CashOutEntity();
        BeanUtils.copyProperties(param,cashOut);
        Date date = DateUtil.nowDateTime();
        cashOut.setCreateTime(date);
        cashOut.setUpdateTime(date);
        cashOut.setUid(uid);
        boolean save = this.save(cashOut);
        if(!save){
            throw new LinfengException("提现申请提交失败");
        }
        //冻结余额
        AppUserEntity user = userService.getById(cashOut.getUid());
        BigDecimal balance = user.getMoney().subtract(cashOut.getMoneyNumber());
        boolean update = userService.lambdaUpdate().set(AppUserEntity::getMoney, balance)
                .eq(AppUserEntity::getUid, cashOut.getUid())
                .update();
        if(!update){
            throw new LinfengException("余额扣除失败");
        }
        //删除用户信息缓存
        org.aileme.common.redis.utils.RedisUtils.deleteObject("userId:"+user.getUid());
        //记录账单
        String mark="提现取出"+cashOut.getMoneyNumber()+"元";
        billService.expend(user.getUid(), BillDetailEnum.TYPE_4.getDesc(),BillDetailEnum.CATEGORY_1.getValue(),BillDetailEnum.TYPE_4.getValue(),cashOut.getMoneyNumber().doubleValue(),balance.doubleValue(),mark,"", null);
    }

    /**
     * 检查用户是否存在待审核的提现申请
     * @param uid 用户ID
     * @return 存在返回false
     */
    @Override
    public boolean checkIsNormal(Integer uid) {
        List<CashOutEntity> list = this.lambdaQuery()
                .eq(CashOutEntity::getUid, uid)
                .eq(CashOutEntity::getStatus, Constant.CASH_REVIEWED)
                .list();
        return list.size() == 0;
    }

    @Override
    public AppCashInfoResponse getAccountBasicInfo(Integer uid) {
        AppCashInfoResponse response=new AppCashInfoResponse();
        AppUserEntity user = userService.getById(uid);
        if(user.getMoney().equals(BigDecimal.ZERO)){
            response.setCanSubmit(false);
            response.setNowMoney(BigDecimal.ZERO);
        } else if(!checkIsNormal(uid)){
            response.setCanSubmit(false);
            response.setNowMoney(user.getMoney());
        } else {
            response.setCanSubmit(true);
            response.setNowMoney(user.getMoney());
        }
        String canCash = configService.getValue(Constant.CAN_CASH_OUT);
        if(canCash.equals("0")){
            response.setCashOpen(false);
        }else {
            response.setCashOpen(true);
        }
        return response;
    }

    /**
     * 线下打款操作
     * @param cashOut
     */
    @Override
    @DSTransactional
    public void updateCash(CashOutEntity cashOut) {
        CashOutEntity cash = this.getById(cashOut.getId());
        if(!cash.getStatus().equals(Constant.CASH_REVIEWED)){
            throw new LinfengException("请勿重复操作");
        }
        if(cashOut.getStatus().equals(Constant.CASH_REFUSE)){
            //解冻余额
            AppUserEntity user = userService.getById(cashOut.getUid());
            BigDecimal balance = user.getMoney().add(cashOut.getMoneyNumber());
            boolean update = userService.lambdaUpdate().set(AppUserEntity::getMoney, balance)
                    .eq(AppUserEntity::getUid, cashOut.getUid())
                    .update();
            if(!update){
                throw new LinfengException("余额解冻失败");
            }
            //删除用户信息缓存
            org.aileme.common.redis.utils.RedisUtils.deleteObject("userId:"+user.getUid());
            //记录账单
            String mark="提现驳回返还"+cashOut.getMoneyNumber()+"元";
            billService.income(user.getUid(),BillDetailEnum.TYPE_4.getDesc(),BillDetailEnum.CATEGORY_1.getValue(),BillDetailEnum.TYPE_4.getValue(),cashOut.getMoneyNumber().doubleValue(),balance.doubleValue(),mark,"", null);

            String content = StrUtil.format(Constant.CASH_OUT_REFUSE,cashOut.getFeedback());
            messageService.sendMessageNotAsync(0,cashOut.getUid(),0,Constant.PUSHARTICLE,content,Constant.TITLE_CASH);
        }else if(cashOut.getStatus().equals(Constant.CASH_FINISH)){
            //消息通知
            String content = StrUtil.format(Constant.CASH_OUT_PASS);
            messageService.sendMessageNotAsync(0,cashOut.getUid(),0,Constant.PUSHARTICLE,content,Constant.TITLE_CASH);
        }
        cashOut.setUpdateTime(DateUtil.nowDateTime());
        this.updateById(cashOut);
    }

    private void checkAccountMoney(Integer uid, BigDecimal cashMoney){
        AppUserEntity user = userService.getById(uid);
        if(cashMoney.equals(BigDecimal.ZERO) || user.getMoney().equals(BigDecimal.ZERO)){
            throw new LinfengException("余额不足无法提现");
        }
        if(cashMoney.compareTo(user.getMoney())>0){
            throw new LinfengException("余额不足无法提现");
        }
    }

}
