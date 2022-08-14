package com.services.common.annotation;

import com.services.common.domain.util.LocalContext;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

@Provider
@ApiVersion
public class ApiVersionFilter implements ContainerRequestFilter {

    @Inject
    LocalContext localContext;

    @Override
    public void filter(ContainerRequestContext requestContext) {

        String apiVersion = this.getClass().getAnnotation(ApiVersion.class).version();
        localContext.setApiVersion(apiVersion);
    }
}
