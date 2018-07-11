package com.mindstixlabs.web.api.version.management.util;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import com.mindstixlabs.web.api.version.management.annotations.ApiVersion;
import com.mindstixlabs.web.api.version.management.annotations.DisabledApi;
import com.mindstixlabs.web.api.version.management.cache.ApiVersioningCache;
import com.mindstixlabs.web.api.version.management.config.ApiVersioningConfigHolder;
import com.mindstixlabs.web.api.version.management.config.ApiVersioningConstants;

/**
 * <code>ApiVersioningUtility</code> is a utility class for API Versioning.
 * 
 * @author Mindstix Software Labs Pvt. Ltd.
 *         <a href="https://www.mindstix.com">(www.mindstix.com)</a>
 *
 */
@Component("api-versioning-utility")
@DependsOn("api-versioning-config-holder")
public class ApiVersioningUtility {

    private static final Logger logger = LoggerFactory.getLogger(ApiVersioningUtility.class);

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * This method is used to validate if the string value for API version is a
     * valid integer or double value.
     * 
     * @param apiVersion                         The string value for API version.
     * 
     * @return                                   Returns true if given string is a valid version.
     */
    public static boolean isValidApiVersion(String apiVersion) {
        boolean isValidApiVersion;

        if (apiVersion.contains(ApiVersioningConstants.DECIMAL_CHAR)) {
            String decimalPattern = ApiVersioningConstants.DECIMAL_REGEX;
            
            // Checking if api version string is a valid decimal number
            isValidApiVersion = Pattern.matches(decimalPattern, apiVersion);
        } else {
            String decimalPattern = ApiVersioningConstants.INTEGER_REGEX;

            // Checking if api version string is a valid integer
            isValidApiVersion = Pattern.matches(decimalPattern, apiVersion);
        }

        return isValidApiVersion;
    }

    /**
     * This method is used to get RequestMappingInfo for handler method applying
     * API version context to existing request mapping.<br>
     * 
     * This method is also used for API versioning optimization by caching active API versions as per configuration.<br>
     * 
     * @param method                             Handler method for which API versioning context is to be applied.
     * @param handlerType                        Controller class.
     * @param existingRequestMappingInfo         Existing request mapping.
     * @param isApiVersioningFallbackEnabled     Flag to determine if caching for API versions is to be done for fallback.
     * @return
     */
    public RequestMappingInfo getApiVersionedRequestMappingInfoForHandlerMethod(Method method, Class<?> handlerType , RequestMappingInfo existingRequestMappingInfo, boolean isApiVersioningFallbackEnabled) {
        RequestMappingInfo resultantRequestMappingInfo = existingRequestMappingInfo;

        // Extracting bean of handlerType class.
        Object controller = applicationContext.getBean(handlerType);

        // Extracting AutoApiVersion annotation for handler.
        ApiVersion autoApiVersionAnnotation = controller.getClass().getAnnotation(ApiVersion.class);

        if (null == autoApiVersionAnnotation) {
            logger.info("Handler [{}] is not annotated with AutoApiVersion annotation, using default RequestMapping", handlerType.getName());
            // Returning existing RequestMappingInfo instance if handler doesn't have AutoApiVersion annotation.
            return existingRequestMappingInfo;
        } else if (StringUtils.isBlank(autoApiVersionAnnotation.value())) {
            logger.warn("AutoApiVersion annotation for Handler [{}] is missing API version, using default RequestMapping for Handler Method [{}]. The application may fail if conflicting RequestMapping already exists.", handlerType.getName(), method.getName());
            // Returning existing RequestMappingInfo instance if API version passed in annotation is empty/null.
            return existingRequestMappingInfo;
        }

        String apiVersion = autoApiVersionAnnotation.value();

        // Validate apiVersion passed in AutoApiVersion annotation.
        boolean hasValidVersion = isValidApiVersion(apiVersion);

        // Checking if version is a valid double number
        if (Boolean.FALSE.equals(hasValidVersion)) {
            // Returns existing RequestMappingInfo instance if API version passed in AutoApiVersion annotation is invalid.
            logger.warn("AutoApiVersion annotation for Handler [{}] has invalid API version [{}], using default RequestMapping for Handler Method [{}]. The application may fail if conflicting RequestMapping already exists.", handlerType.getName(), apiVersion, method.getName());
            return existingRequestMappingInfo;
        }

        DisabledApi disabledApiAnnotationForController = controller.getClass().getAnnotation(DisabledApi.class);

        for (Method controllerMethod : controller.getClass().getMethods()) {
            // Check if requested handlerMethod name matches with method of fetched handler.
            if (controllerMethod.getName().equals(method.getName())) {
                
                // Extract requestMapping for handler Method.
                RequestMapping requestMappingAnnotationForHandlerMethod = method.getAnnotation(RequestMapping.class);
                
                if (null == requestMappingAnnotationForHandlerMethod) {
                    // Return existing RequestMappingInfo instance if method of handler is not a handlerMethod i.e doesn't have RequestMapping annotation.
                    return existingRequestMappingInfo;
                }

                Set<String> existingMappings = existingRequestMappingInfo.getPatternsCondition().getPatterns();
                String[] apiVersionedMappings = new String[existingMappings.size()];

                Iterator<String> existingMappingsIterator = existingMappings.iterator();
                int count = 0;
                
                // Iterating over existing mappings for handlerMethod.
                while (existingMappingsIterator.hasNext()) {
                    String existingMapping = existingMappingsIterator.next();
                    
                    if (StringUtils.isBlank(existingMapping)) {
                        // Replacing existing mapping with empty string if existing mapping is blank or null.
                        existingMapping = ApiVersioningConstants.EMPTY_STRING;
                    }
                    
                    // Creating apiVersionedMapping by concatenating apiVersionedBaseUrl and existingMapping.
                    String apiVersionedMapping = getApiVersioningBaseUrl(apiVersion).concat(existingMapping);

                    // Preparing cache for API version mappings only if versioning is enabled for app
                    if (isApiVersioningFallbackEnabled) {
                        DisabledApi disabledApiAnnotationForMethod = method.getAnnotation(DisabledApi.class);

                        // Flag to consider version for fallback if fallback is enabled
                        boolean addVersionForFallback = true;

                        // Checking if handler and handler method has DisabledApi annotation
                        if (null != disabledApiAnnotationForController || null != disabledApiAnnotationForMethod) {
                            // Checking if fallback is enabled for disabled APIs.
                            if (Boolean.FALSE.equals(ApiVersioningConfigHolder.DISABLED_API_VERSIONS_FALLBACK_ENABLED)) {
                                // Skipping API version for fallback if fallback is disabled for disabled APIs.
                                addVersionForFallback = false;
                            }
                            
                        }

                        if (addVersionForFallback) {
                            // Adding new version entry to cache for handler mapping if version is not disabled or fallback is enabled for disabled APIs.
                            ApiVersioningCache.cacheApiVersionForMapping(existingMapping, Double.valueOf(apiVersion));
                        } else {
                            logger.debug("Skipping disabled version: [{}] from Cache for handler mapping: [{}]", apiVersion, existingMapping);
                            // Adding version entry to list of disabled APIs.
                            ApiVersioningCache.DISABLED_APIS.add(apiVersionedMapping);
                        }
                    }

                    // Adding apiVersionedMapping String to set having API Versioned Mappings.
                    apiVersionedMappings[count] = apiVersionedMapping;
                    count ++;
                }

                PatternsRequestCondition apiVersionedPatternRequestCondition = new PatternsRequestCondition(apiVersionedMappings);
               
                // Creating API Versioned RequestMappingInfo with apiVersionedPatternRequestCondition.
                resultantRequestMappingInfo = new RequestMappingInfo(apiVersionedPatternRequestCondition,
                                                                     existingRequestMappingInfo.getMethodsCondition(),
                                                                     existingRequestMappingInfo.getParamsCondition(),
                                                                     existingRequestMappingInfo.getHeadersCondition(), 
                                                                     existingRequestMappingInfo.getConsumesCondition(),
                                                                     existingRequestMappingInfo.getProducesCondition(),
                                                                     existingRequestMappingInfo.getCustomCondition()); 
            }
        }

        // Returning resultant request mapping info
        return resultantRequestMappingInfo;
    }

    /**
     * This method is used to get API Versioning base URL with or without passed API version if given API version is null/empty.
     * 
     * @param apiVersion                         API version for controller.
     * @return                                   Returns API Versioning base URL with or without given API version.
     */
    public static String getApiVersioningBaseUrl(String apiVersion) {
        String apiContext = ApiVersioningConfigHolder.API_CONTEXT;

        if (StringUtils.isNotBlank(apiContext)) {
            // Removing start '/'
            if (apiContext.startsWith(ApiVersioningConstants.SLASH)) {
                apiContext = apiContext.substring(1);
            }

            // Removing end '/'
            if (apiContext.endsWith(ApiVersioningConstants.SLASH)) {
                apiContext = apiContext.substring(0, apiContext.length() - 1);
            }
        }

        String versionContext = ApiVersioningConfigHolder.VERSION_CONTEXT;

        if (StringUtils.isNotBlank(versionContext)) {
            // Removing start '/'
            if (versionContext.startsWith(ApiVersioningConstants.SLASH)) {
                versionContext = versionContext.substring(1);
            }

            // Removing end '/'
            if (versionContext.endsWith(ApiVersioningConstants.SLASH)) {
                versionContext = versionContext.substring(0, versionContext.length() - 1);
            }
        }

        String apiVersioningBaseUrl = ApiVersioningConstants.SLASH;

        // Concatenating apiContext if not empty/null.
        if (StringUtils.isNotBlank(apiContext)) {
            apiVersioningBaseUrl = apiVersioningBaseUrl.concat(apiContext);
        }

        // Concatenating versionContext if not empty/null.
        if (StringUtils.isNotBlank(versionContext)) {
            apiVersioningBaseUrl = apiVersioningBaseUrl.concat(ApiVersioningConstants.SLASH).concat(versionContext);
        }

        // Concatenating apiVersion if not empty/null.
        if (StringUtils.isNotBlank(apiVersion)) {
            apiVersioningBaseUrl = apiVersioningBaseUrl.concat(apiVersion);
        }

        return apiVersioningBaseUrl;
    }

}
