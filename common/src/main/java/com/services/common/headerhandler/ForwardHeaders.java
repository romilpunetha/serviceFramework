package com.services.common.headerhandler;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.services.common.constant.GlobalConstant;
import com.services.common.domain.util.ClientIdentityData;
import com.services.common.domain.util.LocalContext;
import com.services.common.enums.ServiceName;
import io.quarkus.logging.Log;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Map;

@ApplicationScoped
public class ForwardHeaders implements ClientHeadersFactory {

    @Inject
    protected LocalContext localContext;

    @ConfigProperty(name = "K8S_APP_NAME", defaultValue = "unknown")
    String serviceName;

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders,
                                                 MultivaluedMap<String, String> clientOutgoingHeaders) {

        Log.debug("Setting headers from local context :" + localContext.getContext().toString());

        for (String key : localContext.getContext().keys()) {
            if (key.equals(GlobalConstant.HEADER_CLIENT_POLICY))
                clientOutgoingHeaders.add(key, new Gson().toJson(localContext.getClientPolicy(), new TypeToken<Map<ServiceName, Map<String, String>>>() {
                }.getType()));
            else if (key.equals(GlobalConstant.HEADER_CLIENT_IDENTITY_DATA))
                clientOutgoingHeaders.add(key, new Gson().toJson(localContext.getClientPolicy(), new TypeToken<ClientIdentityData>() {
                }.getType()));
            else if (key.startsWith(GlobalConstant.FRAMEWORK_HEADER_PREFIX))
                clientOutgoingHeaders.add(key, localContext.get(key));
        }

        clientOutgoingHeaders.add(GlobalConstant.HEADER_SERVICE, serviceName);

        return clientOutgoingHeaders;
    }

}
