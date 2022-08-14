package com.services.common.domain.util.email;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder(toBuilder = true)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Email {

    String source;

    String sourceArn;

    String returnPath;

    String returnPathArn;

    List<String> toAddresses;

    List<String> ccAddresses;

    List<String> bccAddresses;

    List<String> replyToAddresses;

    String subject;

    String htmlEmailBody;

    String textEmailBody;

    String configSetName;
}
