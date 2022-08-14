package com.services.common.constant;

import javax.inject.Singleton;
import java.util.*;


@Singleton
public class GlobalConstant {

    public static final String INDIA_COUNTRY_CALLING_CODE = "+91";
    public static final String COUNTRY_CALLING_CODE_CONNECTOR = "-";
    public static final Set<String> whitelistedDomains = new HashSet<>(Arrays.asList(
            "rediff.com", "aol.com", "live.in", "googlemail.com", "rocketmail.com",
            "privaterelay.appleid.com", "live.com", "icloud.com", "outlook.com", "ymail.com", "rediffmail.com", "yahoo.in",
            "hotmail.com", "yahoo.co.in", "yahoo.com", "gmail.com"
    ));
    public static final List<String> INTERNAL_USERS_EMAIL_DOMAINS = new ArrayList<>(Arrays.asList(""));
    public static final String DELIMITER = "__";
    public static final Long DEFAULT_CACHE_EXPIRY_TIME_IN_MILLISECONDS = 60 * 60 * 1000L;
    public static final String FRAMEWORK_HEADER_PREFIX = "x-framework-";
    public static final String HEADER_TEST_DATA = FRAMEWORK_HEADER_PREFIX + "test";
    public static final String HEADER_TENANT = FRAMEWORK_HEADER_PREFIX + "tenant";
    public static final String HEADER_USER = FRAMEWORK_HEADER_PREFIX + "user";
    public static final String HEADER_EXTERNAL_USER = FRAMEWORK_HEADER_PREFIX + "external-user";
    public static final String HEADER_SESSION = FRAMEWORK_HEADER_PREFIX + "session";
    public static final String HEADER_EXTERNAL_TOKEN = FRAMEWORK_HEADER_PREFIX + "external-token";

    public static final String HEADER_DEVICE_TYPE = FRAMEWORK_HEADER_PREFIX + "device-type";
    public static final String HEADER_CLIENT_VERSION = FRAMEWORK_HEADER_PREFIX + "client-version";
    public static final String HEADER_APP_VERSION = FRAMEWORK_HEADER_PREFIX + "app-version";
    public static final String HEADER_LANGUAGE = FRAMEWORK_HEADER_PREFIX + "language";

    public static final String HEADER_AUTH = FRAMEWORK_HEADER_PREFIX + "auth-header";
    public static final String HEADER_TOKEN = FRAMEWORK_HEADER_PREFIX + "auth-token";

    public static final String HEADER_SERVICE = FRAMEWORK_HEADER_PREFIX + "service";

    public static final String API_VERSION = "apiVersion";

    public static final String TENANT_FRAMEWORK = "default-tenant";

    public static final String HEADER_CLIENT_POLICY = FRAMEWORK_HEADER_PREFIX + "client-policy";

    public static final String HEADER_CLIENT_IDENTITY_DATA = FRAMEWORK_HEADER_PREFIX + "client-identity-data";

    public static final String HEADER_CLIENT_APIKEY = FRAMEWORK_HEADER_PREFIX + "client-apikey";

    public static final String DEFAULT_ERROR_MESSAGE = "Something went wrong";
}
