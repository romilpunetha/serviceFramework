package com.services.common.domain.util;

import com.services.common.constant.GlobalConstant;
import com.services.common.enums.ServiceName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.ObjectUtils;

import javax.enterprise.context.RequestScoped;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.function.Supplier;

@SuperBuilder(toBuilder = true)
@Getter
@Setter
@AllArgsConstructor
@ToString(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@RequestScoped
public class LocalContext {

    @Builder.Default
    Context context = Context.empty();

    @Builder.Default
    @Getter(AccessLevel.NONE)
    Context nonTransferableContext = Context.empty();

    public LocalContext() {
        this.context = Context.empty();
        this.nonTransferableContext = Context.empty();
    }

    public void setIsTestData(@NotNull Boolean isTestData) {
        this.set(GlobalConstant.HEADER_TEST_DATA, isTestData.toString());
    }

    public String getUserId() {
        return context.getOrElse(GlobalConstant.HEADER_USER, () -> null);
    }

    public void setUserId(@NotBlank String userId) {
        this.set(GlobalConstant.HEADER_USER, userId);
    }

    public String getExternalUserId() {
        return context.getOrElse(GlobalConstant.HEADER_EXTERNAL_USER, () -> null);
    }

    public void setExternalUserId(@NotBlank String externalUserId) {
        this.set(GlobalConstant.HEADER_EXTERNAL_USER, externalUserId);
    }

    public String getSessionId() {
        return context.getOrElse(GlobalConstant.HEADER_SESSION, () -> null);
    }

    public void setSessionId(@NotBlank String sessionId) {
        this.set(GlobalConstant.HEADER_SESSION, sessionId);
    }

    public String getTenantId() {
        return context.getOrElse(GlobalConstant.HEADER_TENANT, () -> null);
    }

    public void setTenantId(@NotBlank String tenantId) {
        this.set(GlobalConstant.HEADER_TENANT, tenantId);
    }

    public String getAuthHeader() {
        return context.getOrElse(GlobalConstant.HEADER_AUTH, () -> null);
    }

    public void setAuthHeader(@NotBlank String authHeader) {
        this.set(GlobalConstant.HEADER_AUTH, authHeader);
    }

    public String getAuthToken() {
        return context.getOrElse(GlobalConstant.HEADER_TOKEN, () -> null);
    }

    public void setAuthToken(@NotBlank String authToken) {
        this.set(GlobalConstant.HEADER_TOKEN, authToken);
    }

    public String getExternalToken() {
        return context.getOrElse(GlobalConstant.HEADER_EXTERNAL_TOKEN, () -> null);
    }

    public void setExternalToken(@NotBlank String authToken) {
        this.set(GlobalConstant.HEADER_EXTERNAL_TOKEN, authToken);
    }

    public String getApiVersion() {
        return context.getOrElse(GlobalConstant.API_VERSION, () -> "v1");
    }

    public void setApiVersion(String apiVersion) {
        this.set(GlobalConstant.API_VERSION, apiVersion, "v1");
    }

    public Map<ServiceName, Map<String, String>> getClientPolicy() {
        return context.getOrElse(GlobalConstant.HEADER_CLIENT_POLICY, () -> null);
    }

    public void setClientPolicy(Map<ServiceName, Map<String, String>> policyMap) {
        this.set(GlobalConstant.HEADER_CLIENT_POLICY, policyMap);
    }

    public ClientIdentityData getClientIdentityData() {
        return context.getOrElse(GlobalConstant.HEADER_CLIENT_IDENTITY_DATA, () -> null);
    }

    public void setClientIdentityData(ClientIdentityData clientIdentityData) {
        this.set(GlobalConstant.HEADER_CLIENT_IDENTITY_DATA, clientIdentityData);
    }

    public String getClientApikey() {
        return context.getOrElse(GlobalConstant.HEADER_CLIENT_APIKEY, () -> null);
    }

    public void setClientApikey(String apikey) {
        this.set(GlobalConstant.HEADER_CLIENT_APIKEY, apikey);
    }

    public String getDeviceType() {
        return context.getOrElse(GlobalConstant.HEADER_DEVICE_TYPE, () -> null);
    }

    public void setDeviceType(String deviceType) {
        this.set(GlobalConstant.HEADER_DEVICE_TYPE, deviceType);
    }

    public String getClientVersion() {
        return context.getOrElse(GlobalConstant.HEADER_CLIENT_VERSION, () -> null);
    }

    public void setClientVersion(String clientVersion) {
        this.set(GlobalConstant.HEADER_CLIENT_VERSION, clientVersion);
    }

    public String getAppVersion() {
        return context.getOrElse(GlobalConstant.HEADER_APP_VERSION, () -> null);
    }

    public void setAppVersion(String appVersion) {
        this.set(GlobalConstant.HEADER_APP_VERSION, appVersion);
    }

    public String getCallerService() {
        return context.getOrElse(GlobalConstant.HEADER_SERVICE, () -> null);
    }

    public String isTestData() {
        return context.getOrElse(GlobalConstant.HEADER_TEST_DATA, () -> "false");
    }

    public <T> T get(@NotBlank String key) {
        return this.getOrElse(key, () -> null);
    }

    public <T> T getOrElse(@NotBlank String key, @NotNull Supplier<? extends T> alternativeSupplier) {
        return context.getOrElse(key, alternativeSupplier);
    }

    public <T> T nonTransferableGet(@NotBlank String key) {
        return this.nonTransferableGetOrElse(key, () -> null);
    }

    public <T> T nonTransferableGetOrElse(@NotBlank String key, @NotNull Supplier<? extends T> alternativeSupplier) {
        return nonTransferableContext.getOrElse(key, alternativeSupplier);
    }

    public void set(@NotBlank String key, @NotNull Object value) {
        context.put(key, value);
    }

    public <T> void set(@NotBlank String key, T value, @NotNull T defaultValue) {
        if (!ObjectUtils.isEmpty(value))
            context.put(key, value);
        else
            context.put(key, defaultValue);
    }

    public Boolean nonTransferableContains(String key) {
        return nonTransferableContext.contains(key);
    }

    public void nonTransferableSet(@NotBlank String key, @NotNull Object value) {
        nonTransferableContext.put(key, value);
    }

    public <T> void nonTransferableSet(@NotBlank String key, T value, @NotNull T defaultValue) {
        if (!ObjectUtils.isEmpty(value))
            nonTransferableContext.put(key, value);
        else
            nonTransferableContext.put(key, defaultValue);
    }

    public void delete(@NotBlank String key) {
        context.delete(key);
    }
}