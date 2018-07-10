package com.mindstixlabs.web.api.version.management;

import java.lang.reflect.Method;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.mindstixlabs.web.api.version.management.cache.ApiVersioningCache;
import com.mindstixlabs.web.api.version.management.config.ApiVersioningConfigHolder;
import com.mindstixlabs.web.api.version.management.config.ApiVersioningConstants;
import com.mindstixlabs.web.api.version.management.util.ApiVersioningUtility;

/**
 * <p>ReST API Version manager helps to support multiple versions of ReST APIs in
 * runtime for backward compatibility between Old and New applications running
 * across devices.
 * 
 * <p>Pattern followed for API versioned APIs is
 * /${apiContext}/${versionContext}XX.YY where apiContext and versionContext are
 * configurable.
 * 
 * <p>Here XX can be any numeric number (1 to n Digits) and Y is configurable as
 * per decimal digit support number. for example, v1.1 (support = 1 digit),
 * v1.10 (support = 2 digits), v1.100 (support = 3 digit) etc.
 * 
 * <p><b><i>Note: Version 1.1, 1.10 and 1.100 will be considered as 3 different
 * versions by ReST API Version Manager.</i></b>
 * 
 * <p>This HandlerMapping applies API version context at boot-up time to
 * application wide APIs if applicable and also takes care of disabled APIs as
 * per configuration.
 * 
 * <p>By default, ReST API version manager serves requested version if requested
 * version is not less than minimum supported version else returns
 * {@code 404 Not Found} error.
 * 
 * <p>If API versioning fallback feature is enabled, Version manager looks up for
 * latest available version in application using version-mapping cache if
 * requested version is not available. This provides optimization during
 * fallback of API versions.
 * 
 * @author Mindstix Software Labs Pvt. Ltd.
 *         <a href="https://www.mindstix.com">(www.mindstix.com)</a>
 * 
 * @version 0.0.1
 * 
 * @see {@link RequestMapping}
 * @see {@link RequestMappingHandlerMapping}
 * @see {@link AbstractHandlerMethodMapping}
 */
public class ReSTApiVersionManager extends RequestMappingHandlerMapping {

    private static final Logger logger = LoggerFactory.getLogger(ReSTApiVersionManager.class);

    /**
     * Concatenated string for API context and version context.
     */
    private String apiAndVersionContext;

    /**
     * Length of concatenated string for API context and version context.
     */
    private int apiAndVersionContextLength;

    /**
     * Addition of 1 and Number of '/'s coming in the apiAndVersionContext.
     */
    private int postApiAndVersionContextOrdinal;

    @Autowired
    private ContentNegotiationManager contentNegotiationManager;

    @Autowired
    private ApiVersioningUtility apiVersioningUtility;

    /**
     * This is initializer method used to set default configuration required for
     * API version management at runtime.
     */
    @PostConstruct
    public void init() {

        logger.debug("Initializing ReSTApiVersionManager for ReST API version management.");

        this.setOrder(0);

        this.setRemoveSemicolonContent(false);

        this.setContentNegotiationManager(contentNegotiationManager);

        this.apiAndVersionContext = ApiVersioningUtility.getApiVersioningBaseUrl(ApiVersioningConstants.EMPTY_STRING);

        this.apiAndVersionContextLength = this.apiAndVersionContext.length();

        // Addition of 1 and Number of '/'s coming in the apiAndVersionContext.
        // For example, postApiAndVersionContextOrdinal for /api/v = 2+ 1 = 3
        this.postApiAndVersionContextOrdinal = StringUtils.countMatches(apiAndVersionContext, ApiVersioningConstants.SLASH) + 1;

    }

    /**
     * <p>This method is used to lookup latest available handler method for
     * requested API.
     * 
     * <p>By default, This method serves requested version if requested version is
     * not less than minimum supported version else returns
     * {@code 404 Not Found} error.
     * 
     * <p>If API versioning fallback feature is enabled, This method looks up for
     * latest available version in application using version-mapping cache if
     * requested version is not available. This provides optimization during
     * fallback of API versions.
     * 
     */
    @Override
    protected HandlerMethod lookupHandlerMethod(String lookupPath, HttpServletRequest request) throws Exception {

        logger.debug("Inside ReSTApiVersionManager, received request for lookupPath: [{}]", lookupPath);

        HandlerMethod method = null;

        boolean lookupHandlerMethod = true;

        // Checking if requested API version is disabled.
        if (ApiVersioningCache.DISABLED_APIS.contains(lookupPath)) {
            // Checking if disabled APIs are allowed in the application.
            if (ApiVersioningConfigHolder.ALLOW_DISABLED_API_VERSIONS) {
                logger.info("Disabled APIs are allowed for the application. Looking up handler method for the disabled API: [{}].", lookupPath);
            } else {
                logger.warn("Disabled APIs are not allowed for the application. Aborting request for the disabled API: [{}] with 404-NOT FOUND error.", lookupPath);
                
                // Disabling lookup for requested disabled API version.
                lookupHandlerMethod = false;
            }
        }

        // Handling the scenario when requested version is less than minimum version.
        if (lookupHandlerMethod) {
            // Checking for min version if lookup path contains API and version context.
            if (lookupPath.contains(apiAndVersionContext)) {
                // The afterApiUrl has format as ${requested-api-version}/baseApi. If requested 
                // path has format /api/v1.0/users, afterApiUrl will be 1.0/users.
                String afterApiUrl = ApiVersioningConstants.EMPTY_STRING;

                if (StringUtils.isNotBlank(apiAndVersionContext)) {
                    afterApiUrl = lookupPath.substring(lookupPath.indexOf(apiAndVersionContext) + apiAndVersionContextLength);
                } else {
                    // Handling if API context and version context are empty/blank
                    afterApiUrl = lookupPath.substring(1);
                }

                // Requested versioning in string format.
                String requestedVersionStr = afterApiUrl.substring(0, afterApiUrl.indexOf(ApiVersioningConstants.SLASH));

                // Returning looked up handler method if requestedVersionStr is not a valid version.
                if (ApiVersioningUtility.isValidApiVersion(requestedVersionStr)) {
                    Double requestedVersion = Double.valueOf(requestedVersionStr);

                    if (null != requestedVersion && requestedVersion < ApiVersioningConfigHolder.MIN_VERSION_SUPPORT) {
                        logger.warn("Request received for API: [{}] with version: [{}] lower than minimum supported version: [{}]. Aborting request with 404-NOT FOUND error.", lookupPath, requestedVersion, ApiVersioningConfigHolder.MIN_VERSION_SUPPORT);
                        return method;
                    }
                }
            }

            method = super.lookupHandlerMethod(lookupPath, request);
            
            if (null == method) {
                logger.debug("Handler method is not available for lookup path: [{}].", lookupPath);
            }
        }

        // If handler method for requested path is not available and requested path contains API versioning context.
        if (lookupHandlerMethod && method == null && lookupPath.contains(apiAndVersionContext)) {

            // The afterApiUrl has format as ${requested-api-version}/baseApi. If requested 
            // path has format /api/v1.0/users, afterApiUrl will be 1.0/users.
            String afterApiUrl = ApiVersioningConstants.EMPTY_STRING;

            if (StringUtils.isNotBlank(apiAndVersionContext)) {
                afterApiUrl = lookupPath.substring(lookupPath.indexOf(apiAndVersionContext) + apiAndVersionContextLength);
            } else {
                // Handling if API context and version context are empty/blank
                afterApiUrl = lookupPath.substring(1);
            }

            // Requested versioning in string format.
            String requestedVersionStr = afterApiUrl.substring(0, afterApiUrl.indexOf(ApiVersioningConstants.SLASH));

            // Returning looked up handler method if requestedVersionStr is not a valid version.
            if(Boolean.FALSE.equals(ApiVersioningUtility.isValidApiVersion(requestedVersionStr))) {
                logger.debug("Lookup path: [{}] doesn't contains versioning context or has invalid version: [{}]", lookupPath, requestedVersionStr);
                return method;
            }

            // Base API path removing /${apiContext}/${versionContext}${requested-api-version} from lookupPath.
            String baseLookupPath = afterApiUrl.substring(requestedVersionStr.length());

            String path = afterApiUrl.substring(requestedVersionStr.length() + 1);

            Double requestedVersion = null;

            // Flag to check if requested version is above maximum version supported .
            boolean requestedVersionAboveMax = false;

            try {
                requestedVersion = new Double(requestedVersionStr);

                // Starting lookup from maximum supported version, if requested version is more than maximum supported version.
                if (requestedVersion > ApiVersioningConfigHolder.CURRENT_VERSION_SUPPORT) {
                    logger.debug("Requested version: [{}] for API: [{}] is above maximum supported version: [{}]. Lookup will start from version: [{}]",
                                  requestedVersion, path, ApiVersioningConfigHolder.CURRENT_VERSION_SUPPORT, ApiVersioningConfigHolder.CURRENT_VERSION_SUPPORT);
                    requestedVersionStr = Double.toString(ApiVersioningConfigHolder.CURRENT_VERSION_SUPPORT);
                    requestedVersion = ApiVersioningConfigHolder.CURRENT_VERSION_SUPPORT;
                    requestedVersionAboveMax = true;
                }
            } catch (Exception e) {
                logger.error("Error occurred in lookupHandlerMethod for lookupPath: [{}]", lookupPath, e);
                return method;
            }

            Double previousVersion;

            if (requestedVersionAboveMax) {
                // Making previous version as maximum supported version to start lookup from max. supported version.
                previousVersion = requestedVersion;
            } else {
                // Getting previous API version to lookup.
                previousVersion = getPreviousVersion(requestedVersion, baseLookupPath);
            }

            if (null != previousVersion && previousVersion >= ApiVersioningConfigHolder.MIN_VERSION_SUPPORT) {
                // Generating lookup path with previous version to lookup recursively.
                if (StringUtils.isNotBlank(apiAndVersionContext)) {
                    lookupPath = lookupPath.substring(0, lookupPath.indexOf(apiAndVersionContext)) 
                               + apiAndVersionContext
                               + String.format("%." + ApiVersioningConfigHolder.MAX_DECIMAL_DIGITS_SUPPORT + "f", previousVersion)
                               + ApiVersioningConstants.SLASH
                               + path;
                } else {
                    // Handling if API context and version context is empty/blank
                    lookupPath = ApiVersioningConstants.SLASH
                               + String.format("%." + ApiVersioningConfigHolder.MAX_DECIMAL_DIGITS_SUPPORT + "f", previousVersion)
                               + ApiVersioningConstants.SLASH
                               + path;
                }

                final String lookupFinal = lookupPath;

                return lookupHandlerMethod(lookupPath, new HttpServletRequestWrapper(request) {

                    @Override
                    public String getRequestURI() {
                        return lookupFinal;
                    }

                    @Override
                    public String getServletPath() {
                        return lookupFinal;
                    }
                });

            } else if (ApiVersioningConfigHolder.FALLBACK_RETRY_WITH_BASE_LOOKUP_PATH) {

                int pathIndex = 0;

                // Getting index from where base API path starts
                if (StringUtils.isNotBlank(apiAndVersionContext)) {
                    pathIndex = StringUtils.ordinalIndexOf(lookupPath, ApiVersioningConstants.SLASH, postApiAndVersionContextOrdinal);
                } else {
                    // Handling if API context and version context is empty/blank
                    pathIndex = StringUtils.ordinalIndexOf(lookupPath, ApiVersioningConstants.SLASH, 2);
                }

                // Getting non-versioned base API path as lookup path.
                lookupPath = lookupPath.substring(pathIndex);

                final String lookupFinal = lookupPath;

                logger.debug("Retrying fallback with base lookup path: [{}]", lookupFinal);

                // Looking up for non-versioned base API path
                return lookupHandlerMethod(lookupPath, new HttpServletRequestWrapper(request) {

                    @Override
                    public String getRequestURI() {
                        return lookupFinal;
                    }

                    @Override
                    public String getServletPath() {
                        return lookupFinal;
                    }
                });

            }

        }

        return method;

    }

    /**
     * This method is used to get previous versioning to lookup for current
     * version is not available for API
     * 
     * @param currentVersion                     Current version for API.
     * @param apiPath                     API path for which previous version is to be found.
     * @return                                   Returns previous version on the basis of 
     *                                           decimal digits to be considered.
     */
    private Double getPreviousVersion(final Double currentVersion, String apiPath) {
        List<Double> apiVersionsSupported = ApiVersioningCache.getCachedApiVersionsForMapping(apiPath);
        
        Double previousVersion = null;

        // Iterating list of API versions supported for apiPath to find
        // previous supported version less which is than current version.
        for (Double apiVersion : apiVersionsSupported) {
            if (apiVersion < currentVersion) {
                previousVersion = apiVersion;
                break;
            }
        }

        return previousVersion;
    }

    /**
     * This method is used to get API versioned mapping for handler method if
     * API versioning is enabled for particular handler.
     * 
     * @return                                   Returns API versioned RequestMappingInfo
     */
    @Override
    protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
        RequestMappingInfo existingRequestMappingInfo = super.getMappingForMethod(method, handlerType);

        // API versioning fallback is enabled for ReSTApiVersionManager.
        boolean isApiVersioningFallbackEnabled = true;

        if (handlerType.getName().equals(ApiVersioningConstants.BASEI_ERROR_CONTROLLER_CLASS_NAME)) {
            // If handlerType is basic error controller, return existingRequestMappingInfo
            logger.debug("Returning existing handler mapping for handlerType: [{}]", handlerType.getName());
            return existingRequestMappingInfo;
        } else {
            // Get API Versioned RequestMappingInfo if applicable for handlerMethod.
            RequestMappingInfo apiVersionedRequestMappingInfo = apiVersioningUtility.getApiVersionedRequestMappingInfoForHandlerMethod(method, handlerType, existingRequestMappingInfo, isApiVersioningFallbackEnabled);
            
            return apiVersionedRequestMappingInfo;
        }
    }
}
