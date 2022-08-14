package com.services.common.config;

import org.eclipse.microprofile.config.ConfigProvider;

import javax.inject.Singleton;

@Singleton
public class GlobalEnv {

    public static String NODE_NAME = ConfigProvider.getConfig().getOptionalValue("node.name", String.class).orElse("unknown");

    public static String POD_NAME = ConfigProvider.getConfig().getOptionalValue("pod.name", String.class).orElse("unknown");

    public static String POD_NAMESPACE = ConfigProvider.getConfig().getOptionalValue("pod.namespace", String.class).orElse("unknown");

    public static String POD_IP = ConfigProvider.getConfig().getOptionalValue("pod.ip", String.class).orElse("unknown");

    public static String HOST_IP = ConfigProvider.getConfig().getOptionalValue("host.ip", String.class).orElse("unknown");

    public static String K8S_UUID = ConfigProvider.getConfig().getOptionalValue("k8s.uuid", String.class).orElse("unknown");

    public static String K8S_APP_NAME = ConfigProvider.getConfig().getOptionalValue("k8s.app.name", String.class).orElse("unknown");

    public static String K8S_APP_ENV = ConfigProvider.getConfig().getOptionalValue("k8s.app.env", String.class).orElse("unknown");

    public static final String K8S_APP_VERSION = ConfigProvider.getConfig().getOptionalValue("k8s.app.version", String.class).orElse("unknown");

    public static String K8S_APP_TYPE = ConfigProvider.getConfig().getOptionalValue("k8s.app.type", String.class).orElse("unknown");

    public static String ERROR_NAMESPACE = ConfigProvider.getConfig().getOptionalValue("k8s.app.name", String.class).orElse("unknown");

    public static String DEFAULT_ERROR_CODE = ConfigProvider.getConfig().getOptionalValue("default.error.code", String.class).orElse("unknown");

    public static String DEFAULT_ERROR_MESSAGE = ConfigProvider.getConfig().getOptionalValue("default.error.message", String.class).orElse("unknown");


    public static final String DEFAULT_GROUP_NAME = "-1";
    public static final String REQUEST_METHOD = "requestMethod";
    public static final String REQUEST_URL = "requestUrl";
    public static final String REQUEST_BODY = "requestBody";
    public static final String REQUEST_HEADERS = "requestHeaders";
    public static final String RESPONSE_URL = "responseUrl";
    public static final String RESPONSE_BODY = "responseBody";
    public static final String RESPONSE_HEADERS = "responseHeaders";
    public static final String RESPONSE_STATUS = "responseStatus";
    public static final String RESPONSE_STATUS_TEXT = "responseStatusText";


}
