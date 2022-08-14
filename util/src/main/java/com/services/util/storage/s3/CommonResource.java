package com.services.util.storage.s3;

import com.services.common.domain.util.FormData;
import io.quarkus.arc.properties.IfBuildProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@FieldDefaults(level = AccessLevel.PRIVATE)
@IfBuildProperty(name = "framework.aws.s3.enabled", stringValue = "true")
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
public abstract class CommonResource {
    private final static String TEMP_DIR = System.getProperty("java.io.tmpdir");

    @NonNull
    String bucketName;

    @NonNull
    String region;

    String endpointURLOverride;

    protected File getFilePath() {
        return new File(TEMP_DIR, "s3AsyncDownloadedTemp" +
                (new Date()).getTime() + UUID.randomUUID() +
                "." + ".tmp");
    }

    protected ListObjectsRequest buildListObjectRequest() {
        return ListObjectsRequest.builder()
                .bucket(bucketName)
                .build();
    }

    protected GetObjectRequest buildGetRequest(String objectKey) {
        return GetObjectRequest.builder()
                .bucket(this.bucketName)
                .key(objectKey)
                .build();
    }

    protected PutObjectRequest buildPutRequest(String objectKey) {
        return PutObjectRequest.builder()
                .bucket(this.bucketName)
                .key(objectKey)
                .contentType("text/plain")
                .build();
    }

    protected PutObjectRequest buildPutRequest(FormData formData) {
        return PutObjectRequest.builder()
                .bucket(this.bucketName)
                .key(formData.fileName)
                .contentType(formData.mimeType)
                .build();
    }


    protected GetObjectPresignRequest buildPreSignedGetRequest(GetObjectRequest getObjectRequest) {
        return GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .getObjectRequest(getObjectRequest)
                .build();
    }


    protected PutObjectPresignRequest buildPreSignedPutRequest(PutObjectRequest putObjectRequest) {
        return PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(putObjectRequest)
                .build();
    }


    protected PresignedPutObjectRequest getPreSignedRequest(PutObjectPresignRequest putObjectPresignRequest) {
        S3Presigner.Builder s3PresignerBuilder = S3Presigner.builder()
                .region(Region.of(this.region));

        if (!StringUtils.isEmpty(this.endpointURLOverride)) {
            s3PresignerBuilder.endpointOverride(URI.create(this.endpointURLOverride));
        }

        S3Presigner s3Presigner = s3PresignerBuilder.build();

        PresignedPutObjectRequest presignedPutObjectRequest = s3Presigner.presignPutObject(putObjectPresignRequest);

        s3Presigner.close();

        return presignedPutObjectRequest;
    }

    protected PresignedGetObjectRequest getPreSignedRequest(GetObjectPresignRequest getObjectPresignRequest) {
        S3Presigner.Builder s3PresignerBuilder = S3Presigner.builder()
                .region(Region.of(this.region));

        if (!StringUtils.isEmpty(this.endpointURLOverride)) {
            s3PresignerBuilder.endpointOverride(URI.create(this.endpointURLOverride));
        }

        S3Presigner s3Presigner = s3PresignerBuilder.build();

        PresignedGetObjectRequest presignedGetObjectRequest = s3Presigner.presignGetObject(getObjectPresignRequest);

        s3Presigner.close();

        return presignedGetObjectRequest;
    }

}
