package org.aileme.common.oss.core;

import org.aileme.common.oss.entity.UploadResult;
import org.aileme.common.oss.properties.OssProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OssClientTest {

    @Test
    @Tag("dev")
    void shouldKeepCustomQiniuDomainWithoutDuplicatingProtocol() {
        OssProperties properties = new OssProperties();
        properties.setEndpoint("s3-cn-east-1.qiniucs.com");
        properties.setDomain("https://qn.ai-ni.store");
        properties.setBucketName("xg-1306483003");
        properties.setIsHttps("Y");

        assertEquals("https://qn.ai-ni.store", OssClient.buildDomain(properties));
        assertEquals("https://qn.ai-ni.store", OssClient.buildBaseUrl(properties));
    }

    @Test
    @Tag("dev")
    void shouldAppendBucketPathForBucketPrefixedQiniuCustomDomain() {
        OssProperties properties = new OssProperties();
        properties.setEndpoint("xg-1306483003.s3.cn-east-1.qiniucs.com");
        properties.setDomain("https://qn.ai-ni.store");
        properties.setBucketName("xg-1306483003");
        properties.setIsHttps("Y");

        assertEquals("https://qn.ai-ni.store", OssClient.buildDomain(properties));
        assertEquals("https://qn.ai-ni.store/xg-1306483003", OssClient.buildBaseUrl(properties));
    }

    @Test
    @Tag("dev")
    void shouldNormalizeBucketPrefixedQiniuEndpointForSdkAndUrl() {
        OssProperties properties = new OssProperties();
        properties.setEndpoint("https://xg-1306483003.s3.cn-east-1.qiniucs.com");
        properties.setBucketName("xg-1306483003");
        properties.setIsHttps("Y");

        assertEquals("https://s3.cn-east-1.qiniucs.com", OssClient.buildClientEndpoint(properties));
        assertEquals("https://xg-1306483003.s3.cn-east-1.qiniucs.com", OssClient.buildBaseUrl(properties));
    }

    @Test
    @Tag("dev")
    void shouldTreatMissingAccessPolicyAsPublicForLegacyConfig() {
        OssProperties properties = new OssProperties();
        properties.setEndpoint("s3-cn-east-1.qiniucs.com");
        properties.setBucketName("xg-1306483003");
        properties.setAccessKey("demo-access-key");
        properties.setSecretKey("demo-secret-key");
        properties.setIsHttps("Y");

        OssClient client = new OssClient("qiniu", properties);

        assertDoesNotThrow(client::getAccessPolicy);
        assertEquals("1", client.getAccessPolicy().getType());
    }

    @Test
    @Tag("dev")
    void shouldUploadStreamBytesThroughAsyncClient() {
        OssProperties properties = new OssProperties();
        properties.setEndpoint("xg-1306483003.s3.cn-east-1.qiniucs.com");
        properties.setDomain("https://qn.ai-ni.store");
        properties.setBucketName("xg-1306483003");
        properties.setAccessKey("demo-access-key");
        properties.setSecretKey("demo-secret-key");
        properties.setIsHttps("Y");

        S3AsyncClient asyncClient = mock(S3AsyncClient.class);
        S3TransferManager transferManager = mock(S3TransferManager.class);
        S3Presigner presigner = mock(S3Presigner.class);
        when(asyncClient.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
            .thenReturn(CompletableFuture.completedFuture(PutObjectResponse.builder().eTag("etag-demo").build()));

        OssClient client = new OssClient("qiniu", properties, asyncClient, transferManager, presigner);

        UploadResult result = client.upload(
            new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)),
            "oss/test.png",
            5L,
            "image/png"
        );

        assertEquals("https://qn.ai-ni.store/xg-1306483003/oss/test.png", result.getUrl());
        assertEquals("oss/test.png", result.getFilename());
        assertEquals("etag-demo", result.getETag());
    }
}
