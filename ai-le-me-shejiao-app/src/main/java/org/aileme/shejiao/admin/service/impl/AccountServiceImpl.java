package org.aileme.shejiao.admin.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.aileme.common.redis.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.admin.dao.AccountDao;
import org.aileme.shejiao.api.service.AccountService;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.Constant;
import org.aileme.shejiao.common.utils.DateUtil;
import org.aileme.shejiao.common.utils.RedisKeys;
import org.aileme.shejiao.domain.entity.admin.AccountEntity;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;

import java.math.BigDecimal;

@DS("master")
@Service("accountService")
public class AccountServiceImpl extends ServiceImpl<AccountDao, AccountEntity> implements AccountService {

    @Autowired
    private AppUserService appUserService;

    @Override
    public BigDecimal getCoinBalance(Integer uid) {
        AccountEntity account = loadOrInitCoinAccount(uid);
        AppUserEntity user = appUserService.getById(uid);
        BigDecimal userBalance = normalizeUserCoin(user);
        BigDecimal accountBalance = normalizeBalance(account.getBalance());
        if (user != null && accountBalance.compareTo(userBalance) != 0) {
            account.setBalance(userBalance);
            account.setUpdateTime(DateUtil.nowDateTime());
            this.updateById(account);
            return userBalance;
        }
        return accountBalance;
    }

    @Override
    @DSTransactional
    public BigDecimal increaseCoin(Integer uid, int amount) {
        if (amount < 0) {
            throw new LinfengException("增加爱情币数量不合法");
        }
        AccountEntity account = loadOrInitCoinAccount(uid);
        BigDecimal after = normalizeBalance(account.getBalance()).add(BigDecimal.valueOf(amount));
        account.setBalance(after);
        account.setUpdateTime(DateUtil.nowDateTime());
        if (!this.updateById(account)) {
            throw new LinfengException("更新账户余额失败");
        }
        syncUserCoin(uid, after.intValue());
        return after;
    }

    @Override
    @DSTransactional
    public BigDecimal decreaseCoin(Integer uid, int amount) {
        if (amount < 0) {
            throw new LinfengException("扣减爱情币数量不合法");
        }
        AccountEntity account = loadOrInitCoinAccount(uid);
        BigDecimal current = normalizeBalance(account.getBalance());
        BigDecimal delta = BigDecimal.valueOf(amount);
        if (current.compareTo(delta) < 0) {
            throw new LinfengException("爱情币余额不足");
        }
        BigDecimal after = current.subtract(delta);
        account.setBalance(after);
        account.setUpdateTime(DateUtil.nowDateTime());
        if (!this.updateById(account)) {
            throw new LinfengException("更新账户余额失败");
        }
        syncUserCoin(uid, after.intValue());
        return after;
    }

    @Override
    @DSTransactional
    public BigDecimal freezeCoin(Integer uid, int amount) {
        if (amount < 0) {
            throw new LinfengException("冻结爱情币数量不合法");
        }
        AccountEntity account = loadOrInitCoinAccount(uid);
        BigDecimal current = normalizeBalance(account.getBalance());
        BigDecimal delta = BigDecimal.valueOf(amount);
        if (current.compareTo(delta) < 0) {
            throw new LinfengException("爱情币余额不足");
        }
        BigDecimal after = current.subtract(delta);
        BigDecimal frozenAfter = normalizeBalance(account.getFrozenBalance()).add(delta);
        account.setBalance(after);
        account.setFrozenBalance(frozenAfter);
        account.setUpdateTime(DateUtil.nowDateTime());
        if (!this.updateById(account)) {
            throw new LinfengException("冻结账户余额失败");
        }
        syncUserCoin(uid, after.intValue());
        return after;
    }

    @Override
    @DSTransactional
    public BigDecimal unfreezeCoin(Integer uid, int amount) {
        if (amount < 0) {
            throw new LinfengException("解冻爱情币数量不合法");
        }
        AccountEntity account = loadOrInitCoinAccount(uid);
        BigDecimal frozen = normalizeBalance(account.getFrozenBalance());
        BigDecimal delta = BigDecimal.valueOf(amount);
        if (frozen.compareTo(delta) < 0) {
            throw new LinfengException("冻结爱情币不足");
        }
        BigDecimal after = normalizeBalance(account.getBalance()).add(delta);
        BigDecimal frozenAfter = frozen.subtract(delta);
        account.setBalance(after);
        account.setFrozenBalance(frozenAfter);
        account.setUpdateTime(DateUtil.nowDateTime());
        if (!this.updateById(account)) {
            throw new LinfengException("解冻账户余额失败");
        }
        syncUserCoin(uid, after.intValue());
        return after;
    }

    @Override
    @DSTransactional
    public BigDecimal consumeFrozenCoin(Integer uid, int amount) {
        if (amount < 0) {
            throw new LinfengException("消费冻结爱情币数量不合法");
        }
        AccountEntity account = loadOrInitCoinAccount(uid);
        BigDecimal frozen = normalizeBalance(account.getFrozenBalance());
        BigDecimal delta = BigDecimal.valueOf(amount);
        if (frozen.compareTo(delta) < 0) {
            throw new LinfengException("冻结爱情币不足");
        }
        BigDecimal frozenAfter = frozen.subtract(delta);
        BigDecimal balance = normalizeBalance(account.getBalance());
        account.setFrozenBalance(frozenAfter);
        account.setUpdateTime(DateUtil.nowDateTime());
        if (!this.updateById(account)) {
            throw new LinfengException("消费冻结爱情币失败");
        }
        syncUserCoin(uid, balance.intValue());
        return balance;
    }

    @Override
    @DSTransactional
    public void syncCoinFromUser(Integer uid) {
        AppUserEntity user = appUserService.getById(uid);
        AccountEntity account = loadOrInitCoinAccount(uid);
        BigDecimal userBalance = normalizeUserCoin(user);
        account.setBalance(userBalance);
        account.setUpdateTime(DateUtil.nowDateTime());
        this.updateById(account);
    }

    private AccountEntity loadOrInitCoinAccount(Integer uid) {
        AccountEntity account = this.lambdaQuery()
                .eq(AccountEntity::getUid, uid)
                .eq(AccountEntity::getAssetType, Constant.ACCOUNT_ASSET_COIN)
                .one();
        if (account != null) {
            return account;
        }
        AppUserEntity user = appUserService.getById(uid);
        AccountEntity entity = new AccountEntity();
        entity.setUid(uid);
        entity.setAssetType(Constant.ACCOUNT_ASSET_COIN);
        entity.setBalance(normalizeUserCoin(user));
        entity.setFrozenBalance(BigDecimal.ZERO);
        entity.setStatus(Constant.NORMAL);
        entity.setVersion(0);
        entity.setCreateTime(DateUtil.nowDateTime());
        entity.setUpdateTime(DateUtil.nowDateTime());
        this.save(entity);
        return entity;
    }

    private BigDecimal normalizeBalance(BigDecimal balance) {
        return balance == null ? BigDecimal.ZERO : balance;
    }

    private BigDecimal normalizeUserCoin(AppUserEntity user) {
        if (user == null || user.getIntegral() == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(user.getIntegral());
    }

    private void syncUserCoin(Integer uid, Integer balance) {
        appUserService.lambdaUpdate()
                .eq(AppUserEntity::getUid, uid)
                .set(AppUserEntity::getIntegral, balance)
                .update();
        RedisUtils.deleteObject(RedisKeys.getUserCacheKey(uid));
    }
}
