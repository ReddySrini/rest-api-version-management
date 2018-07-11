package com.mindstixlabs.web.api.version.management.config;

import java.lang.annotation.Annotation;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * <code>ApiVersioningConfigHelper</code> is used to load configuration values
 * from properties or annotation which are used for API versioning.
 * 
 * @author Mindstix Software Labs Pvt. Ltd.
 *         <a href="https://www.mindstix.com">(www.mindstix.com)</a>
 */
@Component("api-versioning-config-helper")
public class ApiVersioningConfigHelper {

    private static final Logger logger = LoggerFactory.getLogger(ApiVersioningConfigHelper.class);

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * This method is used to load value from annotation param. If the value is
     * actual string value, it is returned as it. If the value is injected as
     * property, the value is loaded from config properties.
     * 
     * @param annotation                         Annotation for which param value is to be loaded.
     * @param param                              Param for which value is to be loaded.
     * 
     * @return                                   Returns actual value or value loaded from config properties
     */
    public String getValueForAnnotationParam(Annotation annotation, String param) {
        // If param starts with '$', return value for the property from properties file.
        if (param.startsWith(ApiVersioningConstants.PROPERTY_PREFIX)) {
            if (Pattern.matches(ApiVersioningConstants.PROPERTY_PATTERN_REGEX, param)) {
                return applicationContext.getEnvironment().getProperty(param.substring(2, param.length() - 1));
            } else {
                logger.warn("Invalid property key {} is given for annotation {}, Returning empty value.", param, annotation.getClass().getName());
                return ApiVersioningConstants.EMPTY_STRING;
            }
        }

        // Returns actual value for annotation param
        return param;
    }

    /**
     * This method is used to load value from config using property key.
     * 
     * @param key                                Name of property for which value is to be loaded.
     * @return                                   Returns value loaded for the property key.
     */
    public String getValueForProperty(String key) {
        return applicationContext.getEnvironment().getProperty(key);
    }

    /**
     * This method is used to load value for given key or default value if value
     * for key is not found
     * 
     * @param key Property key
     * @param defaultValue Default value for property
     *
     * @return Returns value for property key.
     */
    public String getValueForProperty(String key, String defaultValue) {
        if (StringUtils.isNotBlank(key)) {
            return applicationContext.getEnvironment().getProperty(key, defaultValue);
        }

        return null;
    }

    /**
     * This method is used to load value with given type for given key or default value if
     * value for key is not found
     * 
     * @param key                                Property key
     * @param defaultValue                       Default value for property
     * @param targetType                         Target type to which value is to be converted.
     *
     * @return Returns value for property key with target type.
     */
    public Object getValueForProperty(String key, Object defaultValue, Class<?> targetType) {
        if (StringUtils.isNotBlank(key)) {
                try {
                    return applicationContext.getEnvironment().getRequiredProperty(key, targetType);
                } catch (Exception e) {
                    logger.warn("Value for key: [{}] is invalid. Using default value: [{}]", key, defaultValue);
                }
        } else {
            logger.debug("Found key: [{}] as null/empty. Returning default value: [{}]", key, defaultValue);
        }

        return defaultValue;
    }

}
