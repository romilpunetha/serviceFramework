package com.services.common.domain.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@SuperBuilder(toBuilder = true)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SNSSubscriptionConfirmation {

    @JsonProperty("Message")
    private String message;

    @JsonProperty("MessageId")
    private String messageId;

    @JsonProperty("Signature")
    private String signature;

    @JsonProperty("SignatureVersion")
    private String signatureVersion;

    @JsonProperty("SigningCertURL")
    private String signingCertUrl;

    @JsonProperty("SubscribeURL")
    private String subscribeUrl;

    @JsonProperty("Timestamp")
    private String timestamp;

    @JsonProperty("Token")
    private String token;

    @JsonProperty("TopicArn")
    private String topicArn;

    @JsonProperty("Type")
    private String type;
}

