package com.free.fs.common.storage.platform;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectResult;
import com.free.fs.common.constant.CommonConstant;
import com.free.fs.common.domain.FileBo;
import com.free.fs.common.exception.BusinessException;
import com.free.fs.common.exception.StorageConfigException;
import com.free.fs.common.properties.FsServerProperties;
import com.free.fs.common.storage.IFileStorage;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 阿里云oss文件上传
 *
 * @Author: hao.ding@insentek.com
 * @Date: 2024/1/25 10:32
 */
@Slf4j
public class AliyunOssStorage implements IFileStorage {

    private final OSS client;
    private final String endPoint;
    private final String bucket;

    public AliyunOssStorage(FsServerProperties.AliyunOssProperties config) {
        try {
            String accessKey = config.getAccessKey();
            String secretKey = config.getSecretKey();
            String endPoint = config.getEndpoint();
            String bucket = config.getBucket();
            this.client = new OSSClientBuilder().build(endPoint, secretKey, accessKey);
            this.endPoint = endPoint;
            this.bucket = bucket;
        } catch (Exception e) {
            log.error("[AliyunOSS] OSSClient build failed: {}", e.getMessage());
            throw new StorageConfigException("请检查阿里云OSS配置是否正确");
        }
    }

    @Override
    public boolean bucketExists(String bucket) {
        return false;
    }

    @Override
    public void makeBucket(String bucket) {

    }

    @Override
    public FileBo upload(MultipartFile file) {
        try {
            FileBo fileBo = FileBo.build(file);
            PutObjectResult result = client.putObject(bucket, fileBo.getFileName(), file.getInputStream());
            if (result == null) {
                throw new BusinessException("文件上传失败");
            }
            String url = getUrl(fileBo.getFileName());
            fileBo.setUrl(url);
            return fileBo;
        } catch (Exception e) {
            log.error("[AliyunOSS] file upload failed: {}", e.getMessage());
            throw new BusinessException("文件上传失败");
        } finally {
            client.shutdown();
        }
    }

    @Override
    public void delete(String ObjectName) {
        if (StringUtils.isEmpty(ObjectName)) {
            throw new BusinessException("文件删除失败");
        }
        try {
            client.deleteObject(bucket, ObjectName);
        } catch (Exception e) {
            log.error("[AliyunOSS] file delete failed: {}", e.getMessage());
            throw new BusinessException("文件删除失败");
        } finally {
            client.shutdown();
        }
    }

    @SneakyThrows
    @Override
    public void download(String url, HttpServletResponse response) {

    }

    @Override
    public String getUrl(String ObjectName) {
        return "https://" + bucket + CommonConstant.SUFFIX_SPLIT + endPoint + CommonConstant.DIR_SPLIT + ObjectName;
    }
}

