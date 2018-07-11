package com.mindstixlabs.web.api.version.management.config.rest;

import org.springframework.boot.autoconfigure.web.WebMvcRegistrations;
import org.springframework.boot.autoconfigure.web.WebMvcRegistrationsAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.mindstixlabs.web.api.version.management.DefaultRequestMappingHandlerMapping;
import com.mindstixlabs.web.api.version.management.ReSTApiVersionManager;
import com.mindstixlabs.web.api.version.management.config.ApiVersioningConfigHolder;

/**
 * Configuration class to support API Version Handling.
 * 
 * Spring Boot 1.4.0 onwards, we can specify custom
 * RequestMappingHandlerMapping, while making sure Spring Boot Auto
 * configuration beans still gets auto registered.
 * 
 * http://stackoverflow.com/questions/36744678/spring-boot-swagger-2-ui-custom-requestmappinghandlermapping-mapping-issue
 * https://github.com/spring-projects/spring-boot/issues/5004
 * 
 * 
 * @author Mindstix Software Labs Pvt. Ltd.
 *         <a href="https://www.mindstix.com">(www.mindstix.com)</a>
 */
@Configuration
public class ApiVersioningWebConfiguration {

    /**
     * Overriding RequestMappingHandlerMapping with our custom class called as
     * ReSTApiVersionManager to have logic to manage ReST api version
     * 
     * @return
     */
    @Bean
    @DependsOn("api-versioning-config-holder")
    public WebMvcRegistrations customWebRegistrations() {
        if (ApiVersioningConfigHolder.FEATURE_ENABLED && ApiVersioningConfigHolder.FALLBACK_ENABLED) {
            // Overriding RequestMappingHandlerMapping if versioning feature & fallback is enabled.
            return new WebMvcRegistrationsAdapter() {
                @Override
                public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
                    return new ReSTApiVersionManager();
                }
            };
        } else {
            return new WebMvcRegistrationsAdapter() {
                @Override
                public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
                    return new DefaultRequestMappingHandlerMapping();
                }
            };
        }

    }

}
