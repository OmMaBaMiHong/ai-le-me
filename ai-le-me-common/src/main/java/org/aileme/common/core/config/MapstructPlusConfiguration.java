//package org.aileme.common.core.config;
//
//import org.springframework.boot.autoconfigure.AutoConfiguration;
//import org.springframework.context.annotation.Configuration;
//
//import jakarta.annotation.PostConstruct;
//
///**
// * MapStruct Plus 配置类
// * 用于设置 MapStruct Plus 的系统属性，解决增量编译时创建文件的问题
// */
//@Configuration
//@AutoConfiguration
//public class MapstructPlusConfiguration {
//
//    @PostConstruct
//    public void init() {
//        // 禁用 MapStruct Plus 的增量标记文件创建，避免在用户主目录创建 .msp 文件
//        System.setProperty("msp.increment.mark.disabled", "true");
//    }
//}
