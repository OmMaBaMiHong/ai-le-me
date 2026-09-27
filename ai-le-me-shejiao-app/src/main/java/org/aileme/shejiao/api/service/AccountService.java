package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.domain.entity.admin.AccountEntity;

import java.math.BigDecimal;

public interface AccountService extends IService<AccountEntity> {

    BigDecimal getCoinBalance(Integer uid);

    BigDecimal increaseCoin(Integer uid, int amount);

    BigDecimal decreaseCoin(Integer uid, int amount);

    BigDecimal freezeCoin(Integer uid, int amount);

    BigDecimal unfreezeCoin(Integer uid, int amount);

    BigDecimal consumeFrozenCoin(Integer uid, int amount);

    void syncCoinFromUser(Integer uid);
}
