package com.mindstixlabs.web.api.version.management.config;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/**
 * <code>ApiVersioningPropertyHolder</code> holds configuration being used for API versioning.
 * 
 * @author Mindstix Software Labs Pvt. Ltd.
 *         <a href="https://www.mindstix.com">(www.mindstix.com)</a>
 */
@Component("api-versioning-config-holder")
@DependsOn("api-versioning-config-helper")
public class ApiVersioningConfigHolder {

    private static final Logger logger = LoggerFactory.getLogger(ApiVersioningConfigHolder.class);

    @Autowired
    private ApiVersioningConfigHelper apiVersioningPropertyManager;

    /**
     * The <code>FEATURE_ENABLED</code> is used to specify if we want to
     * enable API versioning for application. If disabled, the bootup time checks won't
     * happen in <code>AutoApiVersionConfigurationManager</code>.
     * 
     */
    public static Boolean FEATURE_ENABLED;

    /**
     * The <code>FALLBACK_ENABLED</code> is used to specify if we want to
     * fallback in case requested API version is not found. Fallback starts from
     * current version to minimum version.
     * 
     */
    public static Boolean FALLBACK_ENABLED;
    
    /**
     * The <code>API_CONTEXT</code> is used specify API context we want to
     * specify for API versioning. for eg. /api, /userapi etc.
     * 
     */
    public static String API_CONTEXT;

    /**
     * The <code>VERSION_CONTEXT</code> is used specify version context we want
     * to specify for API versioning. for eg. /v, /version etc.
     * 
     */
    public static String VERSION_CONTEXT;

    /**
     * The <code>MIN_VERSION_SUPPORT</code> is used to specify minimum version
     * for APIs. If fallback is set as true, it will start fallback from current
     * version to minimum version.
     * 
     */
    public static Double MIN_VERSION_SUPPORT;

    /**
     * The <code>MAX_VERSION_SUPPORT</code> is used to specify current API version.
     * If fallback is set as true, it will start fallback from current version
     * to minimum version.
     * 
     */
    public static Double CURRENT_VERSION_SUPPORT;

    /**
     * The <code>MAX_DECIMAL_DIGITS_SUPPORT</code> is used to specify maximum
     * digits we want to support as part of API version.<br>
     */
    public static Integer MAX_DECIMAL_DIGITS_SUPPORT;

    /**
     * The <code>FALLBACK_RETRY_WITH_BASE_LOOKUP_PATH</code> is used to
     * enable/disable fallback to base lookup path if requested version is less
     * than minimum version support. or API doesn't have active version with max
     * version and minimum version.
     */
    public static Boolean FALLBACK_RETRY_WITH_BASE_LOOKUP_PATH;

    /**
     * The <code>DISABLED_API_VERSIONS_ENABLED</code> is used to
     * enable/disable requests to disabled API versions.
     * 
     */
    public static Boolean ALLOW_DISABLED_API_VERSIONS;

    /**
     * The <code>DISABLED_API_VERSIONS_FALLBACK_ENABLED</code> is used to
     * enable/disable fallback to disabled API versions.
     * 
     * Considered as false if <code>ALLOW_DISABLED_API_VERSIONS</code> is false.
     */
    public static Boolean DISABLED_API_VERSIONS_FALLBACK_ENABLED;

    @PostConstruct
    public void init() {
        // Loading optional configuration from properties or using default values.
        FEATURE_ENABLED = (Boolean) apiVersioningPropertyManager.getValueForProperty(ApiVersioningConfigKeys.FEATURE_EANBLED, ApiVersioningDefaultConfig.FEATURE_ENABLED, Boolean.class);
        
        if (FEATURE_ENABLED) {
            // Enabling fallback for API versioning by default if API versioning feature is enabled.
            FALLBACK_ENABLED = true;

            // Overriding fallback flag for API versioning from properties, keeping default value as true.
            FALLBACK_ENABLED = (Boolean) apiVersioningPropertyManager.getValueForProperty(ApiVersioningConfigKeys.FALLBACK_ENABLED, FALLBACK_ENABLED, Boolean.class);
        } else {
            // Force disabling Versioning fallback if API Versioning feature is disabled.
            FALLBACK_ENABLED = false;

            logger.warn("API versioning feature is disabled for the application. Force disabling API versioning fallback.");
        }

        API_CONTEXT = (String) apiVersioningPropertyManager.getValueForProperty(ApiVersioningConfigKeys.API_CONTEXT, ApiVersioningDefaultConfig.API_CONTEXT, String.class);
        VERSION_CONTEXT = (String) apiVersioningPropertyManager.getValueForProperty(ApiVersioningConfigKeys.VERSION_CONTEXT, ApiVersioningDefaultConfig.VERSION_CONTEXT, String.class);
        MAX_DECIMAL_DIGITS_SUPPORT = (Integer) apiVersioningPropertyManager.getValueForProperty(ApiVersioningConfigKeys.MAX_DECIMANL_DIGITS_SUPPORT, ApiVersioningDefaultConfig.MAX_DECIMAL_DIGIT_SUPPORT, Integer.class);
        FALLBACK_RETRY_WITH_BASE_LOOKUP_PATH = (Boolean) apiVersioningPropertyManager.getValueForProperty(ApiVersioningConfigKeys.FALLBACK_RETRY_WITH_BASE_LOOKUP_PATH, ApiVersioningDefaultConfig.FALLBACK_RETRY_WITH_BASE_LOOKUP_PATH, Boolean.class);
        ALLOW_DISABLED_API_VERSIONS = (Boolean) apiVersioningPropertyManager.getValueForProperty(ApiVersioningConfigKeys.ALLOW_DISABLED_API_VERSIONS, ApiVersioningDefaultConfig.ALLOW_DISABLED_API_VERSIONS, Boolean.class);

        if (ALLOW_DISABLED_API_VERSIONS) {
            // Enabling fallback for disabled API versions by default if disabled API versions are allowed.
            DISABLED_API_VERSIONS_FALLBACK_ENABLED = true;

            // Overriding fallback flag for disabled API versions from properties, keeping default value as true.
            DISABLED_API_VERSIONS_FALLBACK_ENABLED = (Boolean) apiVersioningPropertyManager.getValueForProperty(ApiVersioningConfigKeys.DISABLED_API_VERSIONS_FALLBACK_ENABLED, DISABLED_API_VERSIONS_FALLBACK_ENABLED, Boolean.class);
        } else {
            // Force disabling fallback for disabled API versions if not allowed.
            DISABLED_API_VERSIONS_FALLBACK_ENABLED = false;

            logger.warn("Disabled API versions are not allowed for the application. Force disabling fallback for disabled API versions.");
        }

    }

    @Value("${rest.api.version.management.min.version.support}")
    private void setMinVersionSupport(Double minVersionSupport) {
        if (null != minVersionSupport) {
            MIN_VERSION_SUPPORT = minVersionSupport;
        } else {
            MIN_VERSION_SUPPORT = ApiVersioningDefaultConfig.MIN_VERSION_SUPPORT;
            logger.warn("Invalid value: [{}] for minimum version support key: [api.versioning.min.version.support].");
        }
    }

    @Value("${rest.api.version.management.current.version.support}")
    private void setCurrentVersion(Double currentVersionSupport) {
        if (null != currentVersionSupport) {
            CURRENT_VERSION_SUPPORT = currentVersionSupport;
        } else {
            CURRENT_VERSION_SUPPORT = ApiVersioningDefaultConfig.CURRENT_VERSION_SUPPORT;
            logger.warn("Invalid value: [{}] for current version support key: [api.versioning.current.version.support].");
        }
    }

}