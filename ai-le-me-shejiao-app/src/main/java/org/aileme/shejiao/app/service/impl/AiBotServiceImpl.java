package org.aileme.shejiao.app.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.AiBotService;
import org.aileme.shejiao.app.dao.AiBotDao;
import org.aileme.shejiao.domain.entity.app.AiBot;

/**
 * AI机器人服务实现类
 * @author lww
 * @since 2023-06-10
 */
@DS("master")
@Service("aiBotService")
public class AiBotServiceImpl extends ServiceImpl<AiBotDao, AiBot> implements AiBotService {

}
