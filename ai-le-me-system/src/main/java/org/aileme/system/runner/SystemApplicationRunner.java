package org.aileme.system.runner;

import org.aileme.system.service.ISysThirdPartyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 初始化 system 模块对应业务数据
 *
 * @author Lion Li
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SystemApplicationRunner implements ApplicationRunner {

    private final ISysThirdPartyService thirdPartyService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        thirdPartyService.initRuntimeCaches();
        log.info("初始化第三方运行期缓存成功");
    }

}
