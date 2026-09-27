package org.aileme;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 启动程序
 *
 * @author Lion Li
 */
@Slf4j
@EnableScheduling
@SpringBootApplication
public class AilemeApplication {

    public static void main(String[] args) throws UnknownHostException {
        SpringApplication application = new SpringApplication(AilemeApplication.class);
        ConfigurableApplicationContext context = application.run(args);
        Environment env = context.getEnvironment();

        log.info("\n----------------------------------------------------------------\n\t" +
                "(♥◠‿◠)ﾉﾞ爱了么 ლ(´ڡ`ლ) '{}' 单体服务运行成功! 访问连接:\n\t" +
                "Swagger文档: \t\thttp://{}:{}/doc.html\n\t" +
                "数据库监控: \t\thttp://{}:{}/druid\n" +
                "----------------------------------------------------------------",
            env.getProperty("spring.application.name"),
            InetAddress.getLocalHost().getHostAddress(),
            env.getProperty("server.port"),
            "127.0.0.1",
            env.getProperty("server.port"));
    }

}
