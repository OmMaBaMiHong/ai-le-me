package org.aileme.shejiao.app.oss.factory;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import org.aileme.common.oss.core.OssClient;
import org.aileme.common.oss.factory.OssFactory;

import java.io.InputStream;

/**
 * RuoYi-Vue-Plus OSS 兼容层
 * 将旧的 CloudStorageService 接口桥接到新的 RuoYi OSS 系统
 */
public class RuoyiSysClound extends CloudStorageService {
    
    private final OssClient ossClient;
    
    public RuoyiSysClound() {
        this.ossClient = OssFactory.instance();
    }

    @Override
    public String upload(byte[] data, String path) {
        // 从文件路径中提取文件后缀，用于自动识别 contentType
        String suffix = FileUtil.extName(path);
        String contentType = FileUtil.getMimeType(suffix);
        return ossClient.uploadSuffix(data, suffix, contentType).getUrl();
    }

    @Override
    public String uploadSuffix(byte[] data, String suffix) {
        // 使用 Hutool 自动识别 MIME 类型
        String contentType = FileUtil.getMimeType(suffix);
        return ossClient.uploadSuffix(data, suffix, contentType).getUrl();
    }

    @Override
    public String upload(InputStream inputStream, String path) {
        // 从文件路径中提取文件后缀
        String suffix = FileUtil.extName(path);
        String contentType = FileUtil.getMimeType(suffix);
        // 需要先读取流的长度，因为 OssClient 需要 length 参数
        byte[] data = IoUtil.readBytes(inputStream);
        return ossClient.uploadSuffix(data, suffix, contentType).getUrl();
    }

    @Override
    public String uploadSuffix(InputStream inputStream, String suffix) {
        // 使用 Hutool 自动识别 MIME 类型
        String contentType = FileUtil.getMimeType(suffix);
        // 将 InputStream 转为 byte[] 以便获取长度
        byte[] data = IoUtil.readBytes(inputStream);
        return ossClient.uploadSuffix(data, suffix, contentType).getUrl();
    }
}
