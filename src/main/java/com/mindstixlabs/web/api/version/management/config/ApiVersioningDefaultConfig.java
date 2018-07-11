package com.mindstixlabs.web.api.version.management.config;

/**
 * <code>ApiVersioningDefaultConfig<code> holds default configuration used for ReST Services.
 * 
 * @author Mindstix Software Labs Pvt. Ltd.
 *         <a href="https://www.mindstix.com">(www.mindstix.com)</a>
 */
public class ApiVersioningDefaultConfig {

    public static final Boolean FEATURE_ENABLED = true;

    public static final boolean FALLBACK_ENABLED = true;

    public static final String API_CONTEXT = "";
    
    public static final String VERSION_CONTEXT = "";

    public static final double MIN_VERSION_SUPPORT = 0.0;

    public static final Double CURRENT_VERSION_SUPPORT = 0.0;

    public static final int MAX_DECIMAL_DIGIT_SUPPORT = 1;

    public static final boolean VERSIONING_ENFORCEMENT = false;

    public static final String SCAN_PACKAGES = "";

    public static final String IGNORE_PACKAGES = "";

    public static final String IGNORE_CLASSES = "";

    public static final boolean STOP_APP_ON_CHECK_FAIL = false;

    public static final boolean FALLBACK_RETRY_WITH_BASE_LOOKUP_PATH = false;

    public static final boolean ALLOW_DISABLED_API_VERSIONS = false;

    public static final boolean DISABLED_API_VERSIONS_FALLBACK_ENABLED = false;

}
