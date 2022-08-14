package com.services.util.storage.s3;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import software.amazon.awssdk.services.s3.model.S3Object;

@SuperBuilder(toBuilder = true)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class FileObject {

    String objectKey;
    Long size;

    public static FileObject from(S3Object s3Object) {
        FileObject file = new FileObject();
        if (s3Object != null) {
            file.setObjectKey(s3Object.key());
            file.setSize(s3Object.size());
        }
        return file;
    }
}
