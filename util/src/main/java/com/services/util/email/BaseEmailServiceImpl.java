package com.services.util.email;

import com.services.common.domain.util.email.Email;
import com.services.common.domain.util.email.TemplatedEmail;
import io.quarkus.arc.properties.IfBuildProperty;
import io.smallrye.mutiny.Uni;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesAsyncClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendTemplatedEmailRequest;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.Response;
import java.net.URI;

@ApplicationScoped
@FieldDefaults(level = AccessLevel.PRIVATE)
@IfBuildProperty(name = "framework.aws.ses.enabled", stringValue = "true")
@NoArgsConstructor
public class BaseEmailServiceImpl implements BaseEmailService {

    protected SesAsyncClient sesAsyncClient;

    public BaseEmailServiceImpl(String endpoint, String region) {
        this.sesAsyncClient = SesAsyncClient.builder().endpointOverride(URI.create(endpoint)).region(Region.of(region)).build();
    }

    public Uni<Response> send(Email email) {

        SendEmailRequest request = SendEmailRequest.builder()
                .source(email.getSource())
                .sourceArn(email.getSourceArn())
                .returnPath(email.getReturnPath())
                .returnPathArn(email.getReturnPathArn())
                .destination(destination -> destination.toAddresses(email.getToAddresses())
                        .ccAddresses(email.getCcAddresses())
                        .bccAddresses(email.getBccAddresses())
                )
                .replyToAddresses(email.getReplyToAddresses())
                .message(message -> message.subject(content -> content.data(email.getSubject()))
                        .body(body -> body.text(content -> content.data(email.getTextEmailBody()))
                                .html(content -> content.data(email.getHtmlEmailBody())))
                )
                .configurationSetName(email.getConfigSetName())
                .build();

        return Uni.createFrom().completionStage(sesAsyncClient.sendEmail(request))
                .map(sendEmailResponse -> Response.ok().entity(sendEmailResponse.messageId()).build());
    }

    public Uni<Response> sendTemplatedEmail(TemplatedEmail templatedEmail) {

        SendTemplatedEmailRequest request = SendTemplatedEmailRequest.builder()
                .source(templatedEmail.getSource())
                .sourceArn(templatedEmail.getSourceArn())
                .returnPath(templatedEmail.getReturnPath())
                .returnPathArn(templatedEmail.getReturnPathArn())
                .destination(destination -> destination.toAddresses(templatedEmail.getToAddresses())
                        .ccAddresses(templatedEmail.getCcAddresses())
                        .bccAddresses(templatedEmail.getBccAddresses())
                )
                .replyToAddresses(templatedEmail.getReplyToAddresses())
                .template(templatedEmail.getTemplate())
                .templateArn(templatedEmail.getTemplateArn())
                .templateData(templatedEmail.getTemplateData())
                .configurationSetName(templatedEmail.getConfigSetName())
                .build();

        return Uni.createFrom().completionStage(sesAsyncClient.sendTemplatedEmail(request))
                .map(sendTemplatedEmailResponse -> Response.ok().entity(sendTemplatedEmailResponse.messageId()).build());
    }
}
