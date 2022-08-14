package com.services.util.storage.s3;

import com.services.common.domain.util.FormData;
import io.quarkus.arc.properties.IfBuildProperty;
import io.smallrye.mutiny.Uni;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import javax.ws.rs.core.Response;
import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@FieldDefaults(level = AccessLevel.PRIVATE)
@IfBuildProperty(name = "framework.aws.s3.enabled", stringValue = "true")
@NoArgsConstructor
public class S3Service extends CommonResource {

    S3AsyncClient s3AsyncClient;

    public S3Service(S3AsyncClient s3AsyncClient, String bucketName, String region) {
        super(bucketName, region);
        this.s3AsyncClient = s3AsyncClient;
    }

    public S3Service(S3AsyncClient s3AsyncClient, String bucketName, String region, String endpointURLOverride) {
        super(bucketName, region, endpointURLOverride);
        this.s3AsyncClient = s3AsyncClient;
    }

    public Uni<Response> uploadFile(FormData formData) {

        return Uni.createFrom()
                .completionStage(() -> s3AsyncClient.putObject(buildPutRequest(formData), AsyncRequestBody.fromFile(formData.file)))
                .onItem().ignore().andSwitchTo(Uni.createFrom().item(Response.created(null).build()))
                .onFailure().recoverWithItem(th -> {
                    th.printStackTrace();
                    return Response.serverError().build();
                });
    }

    public Uni<Response> downloadFile(String objectKey, File filePath) {

        return Uni.createFrom()
                .completionStage(() -> s3AsyncClient.getObject(buildGetRequest(objectKey), AsyncResponseTransformer.toFile(filePath)))
                .onItem()
                .transform(object -> Response.ok(filePath)
                        .header("Content-Disposition", "attachment;filename=" + objectKey)
                        .header("Content-Type", object.contentType()).build());
    }

    public Uni<Response> downloadFile(String objectKey) {
        File tempFile = getFilePath();

        return downloadFile(objectKey, tempFile);
    }

    public Uni<List<FileObject>> listFiles() {
        ListObjectsRequest listRequest = buildListObjectRequest();

        return Uni.createFrom().completionStage(() -> s3AsyncClient.listObjects(listRequest))
                .onItem().transform(this::toFileItems);
    }

    private List<FileObject> toFileItems(ListObjectsResponse objects) {
        return objects.contents().stream()
                .sorted(Comparator.comparing(S3Object::lastModified).reversed())
                .map(FileObject::from).collect(Collectors.toList());
    }

    public Uni<String> getPreSignedURLForPUT(String keyName) {
        PresignedPutObjectRequest presignedPutObjectRequest = this.getPreSignedRequest(
                buildPreSignedPutRequest(buildPutRequest(keyName)));
        return Uni.createFrom().item(presignedPutObjectRequest.url().toString());
    }

    public Uni<String> getPreSignedURLForGET(String keyName) {
        PresignedGetObjectRequest presignedGetObjectRequest = getPreSignedRequest(
                buildPreSignedGetRequest(buildGetRequest(keyName)));
        return Uni.createFrom().item(presignedGetObjectRequest.url().toString());
    }
}
