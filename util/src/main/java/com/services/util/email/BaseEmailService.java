package com.services.util.email;

import com.services.common.domain.util.email.Email;
import com.services.common.domain.util.email.TemplatedEmail;
import io.smallrye.mutiny.Uni;

import javax.ws.rs.core.Response;

public interface BaseEmailService {

    Uni<Response> send(Email email);

    Uni<Response> sendTemplatedEmail(TemplatedEmail templatedEmail);
}
