package com.mindstixlabs.web.api.version.management.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * <code>ApiVersioningCache</code> is used to cache data required for API Versioning.
 * 
 * @author Mindstix Software Labs Pvt. Ltd.
 *         <a href="https://www.mindstix.com">(www.mindstix.com)</a>
 *
 */
@Component
public class ApiVersioningCache {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiVersioningCache.class);

    /**
     * Map contains list of versions supported for APIs.
     */
    public static Map<String, List<Double>> API_VERSION_MAPPING_CACHE = new HashMap<>();

    /**
     * List contains disabled APIs for versioning using <code>DisabledApi</code> annotation.
     */
    public static List<String> DISABLED_APIS = new ArrayList<>();

    /**
     * This method is used to add new version entry for handler mapping in Cache.
     * 
     * @param handlerMethodMapping               API path for which version entry is to be done.
     * @param apiVersion                         New version entry to be added against API path.
     */
    public static void cacheApiVersionForMapping(String handlerMethodMapping, Double apiVersion) {
        List<Double> apiVersionsSupported = API_VERSION_MAPPING_CACHE.get(handlerMethodMapping);
        
        if (CollectionUtils.isEmpty(apiVersionsSupported)) {
            apiVersionsSupported = new ArrayList<>();
        }

        apiVersionsSupported.add(apiVersion);

        // Sorting list of versions in descending order before adding to cache.
        Collections.sort(apiVersionsSupported, Collections.reverseOrder());
        API_VERSION_MAPPING_CACHE.put(handlerMethodMapping, apiVersionsSupported);

        logger.debug("Version: [{}] is added in Cache for handler mapping: [{}].", apiVersion, handlerMethodMapping);
    }

    /**
     * This method is used to get list of API versions supported for handler mapping in descending order.
     * 
     * @param handlerMethodMapping               API path for which supported versions are to be fetched.
     * @return                                   Returns List of API versions supported for requested API path.
     */
    public static List<Double> getCachedApiVersionsForMapping(String handlerMethodMapping) {
        List<Double> apiVersionsSupported = null;

        apiVersionsSupported = API_VERSION_MAPPING_CACHE.get(handlerMethodMapping);
        
        if (CollectionUtils.isEmpty(apiVersionsSupported)) {
            apiVersionsSupported = new ArrayList<>();
        }

        return apiVersionsSupported;
    }

}
