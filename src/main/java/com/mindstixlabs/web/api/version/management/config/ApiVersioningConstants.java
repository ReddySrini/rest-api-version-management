package com.mindstixlabs.web.api.version.management.config;

/**
 * <code>ReSTConstants<code> holds constants used for ReST Services.
 * 
 * @author Mindstix Software Labs Pvt. Ltd.
 *         <a href="https://www.mindstix.com">(www.mindstix.com)</a>
 */
public class ApiVersioningConstants {
    
    public static final String SLASH = "/";

    public static final String COMMA = ",";

    public static final String EMPTY_STRING = "";

    public static final String DECIMAL_CHAR = ".";

    public static final String PROPERTY_PATTERN_REGEX = "\\$\\{[-a-zA-Z0-9._]+\\}";

    public static final String DECIMAL_REGEX = "([0-9]*)\\.([0-9]*)";

    public static final String INTEGER_REGEX = "([0-9]*)";

    public static final String PROPERTY_PREFIX = "${";

    public static final String BASEI_ERROR_CONTROLLER_CLASS_NAME = "org.springframework.boot.autoconfigure.web.BasicErrorController";

}
