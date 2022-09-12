package com.services.sf.commons.base;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.services.common.constant.GlobalConstant;
import com.services.common.domain.util.ClientIdentityData;
import com.services.common.domain.util.LocalContext;
import com.services.common.enums.ServiceName;
import io.quarkus.logging.Log;
import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;
import java.util.Map;

@Provider
@ApplicationScoped
public class BaseContextProvider {

    @Inject
    protected LocalContext localContext;

    @ServerRequestFilter(priority = Priorities.AUTHENTICATION + 20)
    public void setLocalContext(HttpHeaders headers) {
        Log.debug("Setting local context from headers :" + headers.getRequestHeaders().toString());

        for (String key : headers.getRequestHeaders().keySet()) {
            if (key.equals(GlobalConstant.HEADER_CLIENT_POLICY) && !StringUtils.isEmpty(headers.getRequestHeaders().getFirst(key)))
                localContext.setClientPolicy(new Gson().fromJson(headers.getRequestHeaders().getFirst(key), new TypeToken<Map<ServiceName, Map<String, String>>>() {
                }.getType()));
            else if (key.equals(GlobalConstant.HEADER_CLIENT_IDENTITY_DATA))
                localContext.setClientIdentityData(new Gson().fromJson(headers.getRequestHeaders().getFirst(key), new TypeToken<ClientIdentityData>() {
                }.getType()));
            else if (key.startsWith(GlobalConstant.FRAMEWORK_HEADER_PREFIX) && !StringUtils.isEmpty(headers.getRequestHeaders().getFirst(key)))
                localContext.set(key, headers.getRequestHeaders().getFirst(key));
        }

        localContext.set(GlobalConstant.HEADER_TENANT, headers.getRequestHeaders().getFirst(GlobalConstant.HEADER_TENANT), GlobalConstant.TENANT_FRAMEWORK);
        localContext.set(GlobalConstant.HEADER_TEST_DATA, headers.getRequestHeaders().getFirst(GlobalConstant.HEADER_TEST_DATA), "false");

        Log.debug("Context :" + localContext.getContext().toString());
    }
}