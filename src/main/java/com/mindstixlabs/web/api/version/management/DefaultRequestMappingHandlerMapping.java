package com.mindstixlabs.web.api.version.management;

import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.mindstixlabs.web.api.version.management.annotations.ApiVersion;
import com.mindstixlabs.web.api.version.management.annotations.DisabledApi;
import com.mindstixlabs.web.api.version.management.cache.ApiVersioningCache;
import com.mindstixlabs.web.api.version.management.config.ApiVersioningConfigHolder;
import com.mindstixlabs.web.api.version.management.config.ApiVersioningConstants;
import com.mindstixlabs.web.api.version.management.util.ApiVersioningUtility;

/**
 * <p>This HandlerMapping is invoked if API versioning feature or API versioning
 * fallback feature is disabled for the application.
 * 
 * <p>This HandlerMapping applies API version context at boot-up time to
 * application wide APIs if applicable and also takes care of disabled APIs
 * as per configuration if API versioning feature is enabled.
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
public class DefaultRequestMappingHandlerMapping extends RequestMappingHandlerMapping {

    private static final Logger logger = LoggerFactory.getLogger(DefaultRequestMappingHandlerMapping.class);

    @Autowired
    private ApiVersioningUtility apiVersioningUtility;

    /**
     * <p>This method is used to lookup handler method for requested lookup path if
     * API versioning feature or API versioning fallback feature is disabled.
     * 
     * <p>If the API with requested lookup path is disabled and disabled APIs are
     * not allowed in the application, {@code 404 Not Found} error will be
     * returned else respective handler method will be returned.
     * 
     * <p>Disabled APIs will be considered if the API versioning feature is
     * disabled for the application.
     * 
     * @see {@link DisabledApi}
     */
    @Override
    protected HandlerMethod lookupHandlerMethod(String lookupPath, HttpServletRequest request) throws Exception {

        logger.debug("Inside DefaultRequestMappingHandlerMapping, received request for lookupPath: [{}]", lookupPath);

        HandlerMethod method = null;

        // Flag to determine if lookup is required for requested lookup path.
        boolean lookupHandlerMethod = true;

        // Checking if API versioning feature is enabled for the application.
        if (ApiVersioningConfigHolder.FEATURE_ENABLED) {
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
        } else {
            // Checking if requested API is disabled.
            if (ApiVersioningCache.DISABLED_APIS.contains(lookupPath)) {
                logger.info("API Versioning feature is disabled for the application. Allowing request for the disabled API: [{}]", lookupPath);
            }
        }

        if (lookupHandlerMethod) {
            // Returning handler method for requested lookup path
            method = super.lookupHandlerMethod(lookupPath, request);
        }

        return method;
    }

    /**
     * <p>This method is used to get API versioned context mappings for handler methods if
     * particular handler is annotated with {@link ApiVersion} annotation and has valid numeric
     * version.
     * 
     * @return                                   Returns API versioned RequestMappingInfo if after validations.
     * 
     * @see {@link RequestMappingInfo}
     *
     * @see <p>{@link ApiVersioningUtility#getApiVersionedRequestMappingInfoForHandlerMethod(Method, Class, RequestMappingInfo, boolean)}
     *
     */
    @Override
    protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
        RequestMappingInfo existingRequestMappingInfo = super.getMappingForMethod(method, handlerType);

        // API versioning fallback is disabled for DefaultRequestMappingHandlerMapping.
        boolean isApiVersioningFallbackEnabled = false;

        if (handlerType.getName().equals(ApiVersioningConstants.BASEI_ERROR_CONTROLLER_CLASS_NAME)) {
            // If handlerType is basic error controller, return existingRequestMappingInfo
            logger.debug("Returning existing handler mapping for handlerType: [{}]", handlerType.getName());
            return existingRequestMappingInfo;
        } else {
            // Get API versioned RequestMappingInfo if applicable for handlerMethod.
            RequestMappingInfo apiVersionedRequestMappingInfo = apiVersioningUtility.getApiVersionedRequestMappingInfoForHandlerMethod(method, handlerType, existingRequestMappingInfo, isApiVersioningFallbackEnabled);
            
            return apiVersionedRequestMappingInfo;
        }
    }

}
