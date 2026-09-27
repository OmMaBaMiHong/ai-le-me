package org.aileme.shejiao.app.oss.factory;

/**
 * 文件上传OSS工厂
 */
public final class OSSFactory {

    public static CloudStorageService build() {
        return new RuoyiSysClound();
    }
}
