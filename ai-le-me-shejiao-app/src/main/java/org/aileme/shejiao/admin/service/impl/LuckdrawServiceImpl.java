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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.aileme.shejiao.api.service.*;
import org.aileme.shejiao.common.enums.BillDetailEnum;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.domain.vo.PrizeListResponse;
import org.aileme.shejiao.common.utils.*;
import org.aileme.shejiao.admin.dao.LuckdrawRecordDao;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.LuckdrawRecordEntity;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.aileme.shejiao.admin.dao.LuckdrawDao;
import org.aileme.shejiao.domain.entity.admin.LuckdrawEntity;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;


@DS("master")
@Service("luckdrawService")
public class LuckdrawServiceImpl extends ServiceImpl<LuckdrawDao, LuckdrawEntity> implements LuckdrawService {

    @Autowired
    private AppUserService appUserService;
    @Autowired
    private LuckdrawRecordService luckdrawRecordService;
    @Autowired
    private SysConfigService configService;
    @Autowired
    private BillService billService;
    @Autowired
    private LuckdrawRecordDao luckdrawRecordDao;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<LuckdrawEntity> page = this.page(
                new Query<LuckdrawEntity>().getPage(params),
                new QueryWrapper<>()
        );

        return new PageUtils(page);
    }

    @Override
    public Map<String, Object> getPrize(AppUserEntity appUser) {
        Map<String, Object> map=new HashMap<>();
        AppUserEntity user = appUserService.getById(appUser.getUid());
        Integer surplus = luckdrawRecordService.getSurplus(user);
        String status = configService.getValue(Constant.LUCK_DRAW_STATUS);
        String rule = configService.getValue(Constant.LUCK_DRAW_RULE);
        String integral = configService.getValue(Constant.LUCK_DRAW_INTEGRAL);
        boolean enough=false;
        if(user.getIntegral()>=Integer.valueOf(integral)){
            enough=true;
        }
        map.put("integral",enough);//用户积分是否充足
        map.put("surplus",surplus);//该用户今天剩余 抽奖次数
        map.put("status",Integer.valueOf(status));//活动状态 0=未开启  1=进行中
        map.put("rule",rule);//活动规则
        map.put("record",luckdrawRecordService.getLuckDrawRecordList());//历史抽奖记录最多最新20条 谢谢惠顾不会出现在这里
        map.put("list",getPrizeList(surplus));//奖品列表（最多只有 8个）

        return map;
    }

    /**
     * 用户抽奖
     * @param appUser
     * @return
     */
    @Override
    @DSTransactional
    public Map<String, Object> startLuckDraw(AppUserEntity appUser) {
        AppUserEntity user = appUserService.getById(appUser.getUid());
        //校验用户抽奖积分是否充足
        String integral = configService.getValue(Constant.LUCK_DRAW_INTEGRAL);
        if(user.getIntegral()<Integer.valueOf(integral)){
            throw new LinfengException("积分不足不能抽奖");
        }
        Integer surplus = luckdrawRecordService.getSurplus(user);
        if(surplus<=0){
            throw new LinfengException("今天抽奖次数用完啦");
        }
        //扣除用户积分并记录账单
        user.setIntegral(user.getIntegral()-Integer.valueOf(integral));
        appUserService.saveOrUpdate(user);
        //积分账单插入
        billService.expend(user.getUid(),"抽奖消耗积分", BillDetailEnum.CATEGORY_2.getValue(),
                BillDetailEnum.TYPE_13.getValue(),Integer.valueOf(integral),user.getIntegral().doubleValue(),
                "抽奖消耗积分","", null);
        //权重计算获取抽奖结果
        List<LuckdrawEntity> list = this.lambdaQuery()
                .eq(LuckdrawEntity::getStatus, 1)
                .orderByDesc(LuckdrawEntity::getSort)
                .last("limit 8")
                .list();
        int index = calculatePrize(list);
        LuckdrawEntity luckdraw = list.get(index);
        //插入抽奖记录
        LuckdrawRecordEntity record=new LuckdrawRecordEntity();
        record.setCreateTime(DateUtil.nowDateTime());
        record.setNumber(luckdraw.getNumber());
        record.setPrizeId(luckdraw.getId());
        record.setPrizeImage(luckdraw.getImage());
        record.setUserId(user.getUid());
        record.setPrizeName(luckdraw.getName());
        record.setPrizeType(luckdraw.getPrizeType());
        boolean save = luckdrawRecordService.save(record);
        if(!save){
            throw new LinfengException("抽奖失败");
        }
        Map<String,Object> map=new HashMap<>();
        map.put("id",luckdraw.getId());
        map.put("name",luckdraw.getName());
        map.put("number",luckdraw.getNumber());
        if(luckdraw.getPrizeType().equals(Constant.LUCK_THANKS)){
            map.put("text","很遗憾您没有中奖~");
        }else if(luckdraw.getPrizeType().equals(Constant.LUCK_INTEGRAL)){
            map.put("text","恭喜您获得"+luckdraw.getNumber()+luckdraw.getName());
            //向用户账户添加积分并记录账单
            AppUserEntity users = appUserService.getById(appUser.getUid());
            users.setIntegral(users.getIntegral()+luckdraw.getNumber());
            appUserService.saveOrUpdate(users);
            billService.income(user.getUid(),"抽奖获得积分", BillDetailEnum.CATEGORY_2.getValue(),
                    BillDetailEnum.TYPE_12.getValue(),luckdraw.getNumber(),users.getIntegral().doubleValue(),
                    "抽奖获得积分","", null);
        }else if(luckdraw.getPrizeType().equals(Constant.LUCK_MONEY)){
            map.put("text","恭喜您获得"+luckdraw.getNumber()+"元"+luckdraw.getName());
            //向用户账户添加余额并记录账单
            AppUserEntity users = appUserService.getById(appUser.getUid());
            users.setMoney(new BigDecimal(luckdraw.getNumber()).add(users.getMoney()));
            appUserService.saveOrUpdate(users);
            billService.income(user.getUid(),"抽奖获得红包", BillDetailEnum.CATEGORY_1.getValue(),
                    BillDetailEnum.TYPE_15.getValue(),luckdraw.getNumber(),users.getMoney().doubleValue(),
                    "抽奖获得红包","", null);
        }
        org.aileme.common.redis.utils.RedisUtils.deleteObject("userId:" +user.getUid());
        return map;
    }

    /**
     * 用户抽奖记录 分页
     * @param user
     * @return
     */
    @Override
    public AppPageUtils record(AppUserEntity user,Integer currPage) {
        Page<LuckdrawRecordEntity> page = new Page<>(currPage,10);
        QueryWrapper<LuckdrawRecordEntity> queryWrapper=new QueryWrapper<>();
        queryWrapper.lambda().eq(LuckdrawRecordEntity::getUserId,user.getUid());
        queryWrapper.lambda().orderByDesc(LuckdrawRecordEntity::getId);
        Page<LuckdrawRecordEntity> selectPage = luckdrawRecordDao.selectPage(page, queryWrapper);
        return new AppPageUtils(selectPage);
    }

    /**
     * 获取奖品列表
     * @return
     */
    private List<PrizeListResponse> getPrizeList(Integer surplus){
        List<LuckdrawEntity> list = this.lambdaQuery()
                .eq(LuckdrawEntity::getStatus, 1)
                .orderByDesc(LuckdrawEntity::getSort)
                .last("limit 8")
                .list();
        List<PrizeListResponse> listResponses=new ArrayList<>();
        list.forEach(e->{
            PrizeListResponse response=new PrizeListResponse();
            BeanUtils.copyProperties(e,response);
            response.setUrl(e.getImage());
            response.setType(e.getPrizeType());
            listResponses.add(response);
        });
        //九宫格中心一格单独处理
        PrizeListResponse response=new PrizeListResponse();
        response.setName("今日剩余" + surplus + "次");
        response.setType(1);
        listResponses.add(4,response);
        return listResponses;
    }


    /**
     * 抽奖计算权重
     * @param list
     * @return
     */
    public static int calculatePrize(List<LuckdrawEntity> list){
        double sum = list.stream().mapToDouble(LuckdrawEntity::getProbability).sum();
        double random = Math.random();
        double currentMax = 0;
        double max = 0;
        double min = 0;
        int index = -1;
        for (int i = 0; i < list.size(); i++) {
            currentMax =  list.get(i).getProbability()/sum;
            max  = currentMax + max;

            if(random >= min && random <= max){
                index = i;
                break;
            }
            min = currentMax + min;
        }

        return index;
    }






}
