package com.mindstixlabs.web.api.version.management.handlers;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;

import com.mindstixlabs.web.api.version.management.annotations.ApiVersion;
import com.mindstixlabs.web.api.version.management.annotations.ApiVersionCheck;
import com.mindstixlabs.web.api.version.management.cache.ApiVersioningCache;
import com.mindstixlabs.web.api.version.management.config.ApiVersioningConfigHelper;
import com.mindstixlabs.web.api.version.management.config.ApiVersioningConfigHolder;
import com.mindstixlabs.web.api.version.management.config.ApiVersioningConfigKeys;
import com.mindstixlabs.web.api.version.management.config.ApiVersioningConstants;
import com.mindstixlabs.web.api.version.management.config.ApiVersioningDefaultConfig;
import com.mindstixlabs.web.api.version.management.util.ApiVersioningUtility;

/**
 * Event listener for context refreshed event to capture application context and
 * load controllers for API Versioning configuration.<br>
 * 
 * See <a href=
 * "https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/context/event/ContextRefreshedEvent.html">https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/context/event/ContextRefreshedEvent.html</a>
 * 
 * @author Mindstix Software Labs Pvt. Ltd.
 *         <a href="https://www.mindstix.com">(www.mindstix.com)</a>
 */
@Component
public class AutoApiVersionConfigurationManager implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ContextRefreshedEvent.class);

    @Autowired
    private ApiVersioningConfigHelper apiVersioningPropertyManager;
    
    @Autowired
    private ApplicationContext applicationContext;

    /**
     * List of controllers for which versioning is enabled.
     */
    private List<String> versioningEnabledControllers = new ArrayList<String>();

    /**
     * List of controllers for which versioning is skipped using skipVersion flag.
     */
    private List<String> versioningSkippedControllers = new ArrayList<String>();
    
    /**
     * List of controllers for which versioning is ignored.<br>
     * Includes controllers which belongs to packages that are mentioned in ignorePackages of
     */
    private List<String> versioningIgnoredControllersAtPackageLevel = new ArrayList<String>();
    
    /**
     * List of controllers for which versioning is ignored.<br>
     * Includes controllers which are classes mentioned in ignoreClasses of AutoApiVersionCheck annotation.<br>
     */
    private List<String> versioningIgnoredControllersAtClassLevel = new ArrayList<String>();
    
    /**
     * List of controllers for which versioning is ignored or version params are missed/has invalid value.<br>
     * Includes controllers which are not mentioned in scanPackages of AutoApiVersionCheck annotation.<br>
     */
    private List<String> versioningIgnoredControllersByDisablingVersioningEnforcement = new ArrayList<String>();

    /**
     * List of controllers for which versioning is missing.<br>
     * Included controllers which<br> - belongs to packages to be scanned but are not annotated with AutoApiVersion
     *                                  annotation and not mentioned in ignoredPackages/ignoredClasses.<br>
     *                                - belongs to packages to be scanned but version param is not passed in AutoApiVersion
     */
    private List<String> versioningMissingControllers = new ArrayList<String>();
    
    /**
     * List of controllers for which API version passed in <code>AutoApiVersion</code> annotation is invalid.<br>
      */
    private List<String> versioningInvalidControllers = new ArrayList<String>();
    
    /**
     * Used to fetch the max version used among all the controllers in the application.
     */
    public static double currentVersion = ApiVersioningConfigHolder.CURRENT_VERSION_SUPPORT;

    /**
     * Used to fetch the minimum version used among all the controllers in the application.
     */
    public static double minVersionSupport = ApiVersioningConfigHolder.MIN_VERSION_SUPPORT;

    /**
     * Flag to decide if API versioning is to be enforced for the application.<br>
     * 
     * Value is false if AutoApiVersionCheck annotation is missing or if stopAppOnCheckFail is disabled in AutoApiVersionCheck.
     */
    private boolean enforceApiVersioning = true;
    
    /**
     * Used to stop application if API versioning validation and configuration is failed.
     * 
     */
    private boolean forceStopApp = false;

    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (ApiVersioningConfigHolder.FEATURE_ENABLED) {
            Map<String, Object> autoApiVersionCheckAnnotationMap = applicationContext.getBeansWithAnnotation(ApiVersionCheck.class);

            ApiVersionCheck autoApiVersionCheckAnnotation = null;

            if (null == autoApiVersionCheckAnnotationMap || autoApiVersionCheckAnnotationMap.isEmpty()) {
                logger.warn("AutoApiVersionCheck annotation is missing on application class. Disabling API versioning enforcement for application");

                // Loading default configuration if AutoApiVersionCheck annotation is missing.
                autoApiVersionCheckAnnotation = getDefaultApiVersioningCheckConfiguration();

                // Using default enforcement strategy for API versioning if AutoApiVersionCheck is missing.
                enforceApiVersioning = ApiVersioningDefaultConfig.VERSIONING_ENFORCEMENT;
            } else if (autoApiVersionCheckAnnotationMap.values().size() > 1) {
                List<String> autoApiVersionCheckAnnotatedClasses = autoApiVersionCheckAnnotationMap.keySet().stream().collect(Collectors.toList());
                
                logger.warn("AutoApiVersionCheck is used at multiple classes [{}]. Using default auto-api-versioning configuration.", Arrays.toString(autoApiVersionCheckAnnotatedClasses.toArray()));

                // Using default config if AutoApiVersionCheck annotation is used at multiple places.
                autoApiVersionCheckAnnotation = getDefaultApiVersioningCheckConfiguration();
                
                enforceApiVersioning = autoApiVersionCheckAnnotation.stopAppOnCheckFail();
            } else {
                autoApiVersionCheckAnnotation = AnnotationUtils.findAnnotation(autoApiVersionCheckAnnotationMap.values().iterator().next().getClass(), ApiVersionCheck.class);

                if (autoApiVersionCheckAnnotation != null) {
                    // Setting scanPackages for AutoApiVersionCheck if empty.
                    autoApiVersionCheckAnnotation = setScanPackagesForAutoApiVersionCheck(autoApiVersionCheckAnnotation, applicationContext);

                    String stopAppOnCheckFailString = apiVersioningPropertyManager.getValueForAnnotationParam(autoApiVersionCheckAnnotation, String.valueOf(autoApiVersionCheckAnnotation.stopAppOnCheckFail()));
                    
                    // Using default value as true if AutoApiVersionCheck annotation is used.
                    boolean stopAppOnCheckFail = ApiVersioningDefaultConfig.STOP_APP_ON_CHECK_FAIL;
                    
                    if (StringUtils.isNotBlank(stopAppOnCheckFailString)) {
                        try {
                            stopAppOnCheckFail = Boolean.valueOf(stopAppOnCheckFailString);
                        } catch (Exception e) {
                            logger.error("Invalid value is provided for stopAppOnCheckFail in AutoApiVersionCheck annotation. Using default value as true.");
                        }
                    }
                    
                    enforceApiVersioning = stopAppOnCheckFail;
                } else {
                    enforceApiVersioning = ApiVersioningDefaultConfig.VERSIONING_ENFORCEMENT;
                }
            }

            // Logging auto API versioning configuration
            logApiVersioningConfig(autoApiVersionCheckAnnotation);

            if (enforceApiVersioning) {
                logger.info("Enabling enforcement for auto-api-versioning.");
            } else {
                logger.info("Disabling enforcement for auto-api-versioning.");
            }

            // Extracting application wide controllers and RestControllers
            Map<String, Object> controllers = applicationContext.getBeansWithAnnotation(Controller.class);

            for (Object controller : controllers.values()) {
                // Checking if the controller is annotated with AutoApiVersion annotation.
                if (isApiVersioningAnnotationPresent(controller)) {
                    // Checking if the controller is skipped explicitly using skipVersioning flag.
                    
                    // Checking if skipVersioning flag is disabled
                    if (isVersioningEnabledForController(controller)) {
                        // Extracting AutoApiVersion annotation for controller.
                        ApiVersion autoApiVersionAnnotation = controller.getClass().getAnnotation(ApiVersion.class);

                        String apiVersion = autoApiVersionAnnotation.value();

                        // Validate apiVersion passed in AutoApiVersion annotation.
                        boolean isVersionValid = ApiVersioningUtility.isValidApiVersion(apiVersion);

                        // Checking if version is a valid double number
                        if (Boolean.FALSE.equals(isVersionValid)) {
                            logger.warn("Invalid version [{}] is specified for controller {}", apiVersion, controller.getClass().getName());
                            
                            String controllerWithInvalidVersion = String.format("%s-v%s", controller.getClass().getName(), apiVersion);
                            versioningInvalidControllers.add(controllerWithInvalidVersion);
                            
                            forceStopApp = true;
                            apiVersion = null;
                        }

                        // Checking if blank/empty version is provided for AutoApiVersion annotation
                        if (StringUtils.isBlank(apiVersion)) {
                            // Checking if API versioning is enforced and if controller belongs to packages to be scanned.
                            if (enforceApiVersioning && isControllerBelongsToPackagesToBeScanned(controller, autoApiVersionCheckAnnotation)) {
                                if (Boolean.FALSE.equals(isControllerIgnored(controller, autoApiVersionCheckAnnotation))) {

                                    // If controller is not ignored using ignoredPackages or ignoredClasses.
                                    versioningMissingControllers.add(controller.getClass().getName());

                                    forceStopApp = true;
                                }
                            } else {
                                versioningIgnoredControllersByDisablingVersioningEnforcement.add(controller.getClass().getName());
                            }

                            continue;
                        }

                        // Updating max version support (current version) throughout the application
                        if (Double.valueOf(apiVersion) > currentVersion) {
                            currentVersion = Double.valueOf(apiVersion);
                        }

                        // Updating min version support throughout the application
                        if (Double.valueOf(apiVersion) < minVersionSupport || minVersionSupport == ApiVersioningDefaultConfig.MIN_VERSION_SUPPORT) {
                            minVersionSupport = Double.valueOf(apiVersion);
                        }

                        String controllerWithVersion = String.format("%s-v%s", controller.getClass().getName(), apiVersion);

                        versioningEnabledControllers.add(controllerWithVersion);
                    }
                } else {
                    // Checking if API versioning is enforced and if controller belongs to packages to be scanned
                    if (enforceApiVersioning && isControllerBelongsToPackagesToBeScanned(controller, autoApiVersionCheckAnnotation)) {
                        if (Boolean.FALSE.equals(isControllerIgnored(controller, autoApiVersionCheckAnnotation))) {
                            // If controller is not ignored using ignoredPackages or ignoredClasses.
                            versioningMissingControllers.add(controller.getClass().getName());

                            forceStopApp = true;
                            
                            continue;
                        }
                    } else {
                        versioningIgnoredControllersByDisablingVersioningEnforcement.add(controller.getClass().getName());

                        continue;
                    }
                }
            }

            // If current version support provided in properties is null/empty, using current version loaded from code.
            if (null == ApiVersioningConfigHolder.CURRENT_VERSION_SUPPORT || ApiVersioningConfigHolder.CURRENT_VERSION_SUPPORT == ApiVersioningDefaultConfig.CURRENT_VERSION_SUPPORT) {
                ApiVersioningConfigHolder.CURRENT_VERSION_SUPPORT = currentVersion;
                logger.info("The current version support provided is null/empty. Loaded maximum supported version [{}] determined by the system as current version.", ApiVersioningConfigHolder.CURRENT_VERSION_SUPPORT);
            } else if (ApiVersioningConfigHolder.CURRENT_VERSION_SUPPORT < currentVersion) {
                // If current version provided is less than current version determined by system, logging the warning.
                logger.warn("The current version support provided [{}] is less than current version determined by system [{}]", ApiVersioningConfigHolder.CURRENT_VERSION_SUPPORT, currentVersion);
            }

            // If current version support provided in properties is null/empty, using current version loaded from code.
            if (null == ApiVersioningConfigHolder.MIN_VERSION_SUPPORT || ApiVersioningConfigHolder.MIN_VERSION_SUPPORT == ApiVersioningDefaultConfig.MIN_VERSION_SUPPORT) {
                ApiVersioningConfigHolder.MIN_VERSION_SUPPORT = minVersionSupport;
                logger.info("The minimum version support provided is null/empty. Loaded minimum supported version [{}] determined by the system.", ApiVersioningConfigHolder.MIN_VERSION_SUPPORT);
            }
            
            if (Boolean.FALSE.equals(CollectionUtils.isEmpty(versioningEnabledControllers))) {
                logger.info("Enabling the API Versioning for controllers {}", Arrays.toString(versioningEnabledControllers.toArray()));
            }

            if (Boolean.FALSE.equals(CollectionUtils.isEmpty(versioningSkippedControllers))) {
                logger.info("Skipping the API Versioning validation for controllers {}", Arrays.toString(versioningSkippedControllers.toArray()));
            }
            
            if (Boolean.FALSE.equals(CollectionUtils.isEmpty(versioningIgnoredControllersAtPackageLevel))) {
                logger.info("Ignoring the API Versioning validation at package level for controllers {}", Arrays.toString(versioningIgnoredControllersAtPackageLevel.toArray()));
            }

            if (Boolean.FALSE.equals(CollectionUtils.isEmpty(versioningIgnoredControllersAtClassLevel))) {
                logger.info("Ignoring the API Versioning validation at class level for controllers {}", Arrays.toString(versioningIgnoredControllersAtClassLevel.toArray()));
            }

            if (Boolean.FALSE.equals(CollectionUtils.isEmpty(versioningIgnoredControllersByDisablingVersioningEnforcement))) {
                logger.info("Ignoring the API Versioning validation for controllers {}", Arrays.toString(versioningIgnoredControllersByDisablingVersioningEnforcement.toArray()));
            }

            if (Boolean.FALSE.equals(CollectionUtils.isEmpty(versioningMissingControllers))) {
                logger.warn("API Version is missing for controllers {}", Arrays.toString(versioningMissingControllers.toArray()));
            }

            if (Boolean.FALSE.equals(CollectionUtils.isEmpty(versioningInvalidControllers))) {
                logger.warn("Invalid version is passed for controllers {}", Arrays.toString(versioningInvalidControllers.toArray()));
            }
            
            if (Boolean.FALSE.equals(CollectionUtils.isEmpty(ApiVersioningCache.DISABLED_APIS))) {
                logger.warn("APIs disabled for Versioning {}", Arrays.toString(ApiVersioningCache.DISABLED_APIS.toArray()));
            }
            
            // Calling shutdown method to force stop app
            if (forceStopApp) {
                shutDown(applicationContext);
            }
        } else {
            logger.info("The API Versioning feature is disabled for the application, skipping configurations and validations for API Versioning.");
        }
    }

    /**
     * This method is used to log final auto-api-versioning configuration
     * 
     * @param autoApiVersionCheckAnnotation      AutoApiVersionCheck annotation instance
     */
    private void logApiVersioningConfig(ApiVersionCheck autoApiVersionCheckAnnotation) {
        if (null != autoApiVersionCheckAnnotation) {
            // Logging AutoApiVersionCheck values
            logger.info("API Versioning Config: Packages to scan for versioning are {}.", Arrays.toString(autoApiVersionCheckAnnotation.scanPackages()));
            logger.info("API Versioning Config: Packages to ignore for versioning are {}.", Arrays.toString(autoApiVersionCheckAnnotation.ignorePackages()));
            logger.info("API Versioning Config: Classes to ignore for versioning are {}.", Arrays.toString(autoApiVersionCheckAnnotation.ignoreClasses()));
        }

        // Logging API Versioning configuration value
        logger.info("API Versioning config: Flag to enable API Versioning feature is [{}]", ApiVersioningConfigHolder.FEATURE_ENABLED);
        logger.info("API Versioning config: Flag to enable API Versioning fallback is [{}]", ApiVersioningConfigHolder.FALLBACK_ENABLED);
        logger.info("API Versioning config: API context for Versioning is [{}]", ApiVersioningConfigHolder.API_CONTEXT);
        logger.info("API Versioning config: Version context for API Versioning is [{}]", ApiVersioningConfigHolder.VERSION_CONTEXT);
        logger.info("API Versioning config: Minimum version support for API Versioning is [{}]", ApiVersioningConfigHolder.MIN_VERSION_SUPPORT);
        logger.info("API Versioning config: Current version support for API Versioning is [{}]", ApiVersioningConfigHolder.CURRENT_VERSION_SUPPORT);
        logger.info("API Versioning config: Flag to retry fallback with base lookup path is [{}]", ApiVersioningConfigHolder.FALLBACK_RETRY_WITH_BASE_LOOKUP_PATH);
        logger.info("API Versioning config: Flag to allow disabled API versions is [{}]", ApiVersioningConfigHolder.ALLOW_DISABLED_API_VERSIONS);
        logger.info("API Versioning config: Flag to enable fallback for disabled API versions is [{}]", ApiVersioningConfigHolder.DISABLED_API_VERSIONS_FALLBACK_ENABLED);
    }

    /**
     * This method is used to set scanPackages for AutoApiVersionCheck when scanPackages is empty array. 
     * This method sets basePackages for componentScan annotation as scanPackages for AutoApiVersionCheck.
     * 
     * @param autoApiVersionCheckAnnotation      Annotation instance for which scanPackages is to be net.
     * @param applicationContext                 Application context instance.
     * @return                                   Return annotation with componentScan basePackages as scanPackages. 
     */
    public ApiVersionCheck setScanPackagesForAutoApiVersionCheck(ApiVersionCheck autoApiVersionCheckAnnotation, ApplicationContext applicationContext) {
        // Checking if scanPackages array is empty
        if (null == autoApiVersionCheckAnnotation.scanPackages() || autoApiVersionCheckAnnotation.scanPackages().length == 0) {
            Map<String, Object> componentScanAnnotationMap = applicationContext.getBeansWithAnnotation(ComponentScan.class);

            // List that will contain all the packages to be scan.
            List<String> totalScanPackages = new ArrayList<String>();

            for (Object componentScanAnnotatedBean : componentScanAnnotationMap.values()) {
                ComponentScan componentScanAnnotation = AnnotationUtils.findAnnotation(componentScanAnnotatedBean.getClass(), ComponentScan.class);
                
                // Adding base packages in component scan annotation to list that contains total packages to be scan.
                totalScanPackages.addAll(Arrays.asList(componentScanAnnotation.basePackages()));
            }

            // Converting list to array.
            String[] scanPackages = totalScanPackages.toArray(new String[totalScanPackages.size()]);

            return new ApiVersionCheck() {
                
                @Override
                public Class<? extends Annotation> annotationType() {
                   return annotationType();
                }
                
                @Override
                public String[] value() {
                    return scanPackages;
                }

                @Override
                public String[] scanPackages() {
                    return scanPackages;
                }

                @Override
                public String[] ignorePackages() {
                    return autoApiVersionCheckAnnotation.ignorePackages();
                }
                
                @Override
                public String[] ignoreClasses() {
                    return autoApiVersionCheckAnnotation.ignoreClasses();
                }

                @Override
                public boolean stopAppOnCheckFail() {
                    return autoApiVersionCheckAnnotation.stopAppOnCheckFail();
                }
            };
        } else {
            // Returning annotation value unchanged if scanPackages array is non empty.
            return autoApiVersionCheckAnnotation;
        }
    }
    
    /**
     * This method is used to set default API Versioning Check Configuration if
     * AutoApiVersionCheck annotation used at multiple places and creating
     * conflict.
     * 
     * @param autoApiVersionCheckAnnotation      
     */
    private ApiVersionCheck getDefaultApiVersioningCheckConfiguration() {
        ApiVersionCheck autoApiVersionCheckAnnotation = new ApiVersionCheck() {
            
            @Override
            public Class<? extends Annotation> annotationType() {
                return annotationType();
            }
            
            @Override
            public String[] value() {
                String[] values = (String[]) apiVersioningPropertyManager.getValueForProperty(ApiVersioningConfigKeys.DEFAULT_SCAN_PACKAGES, ApiVersioningDefaultConfig.SCAN_PACKAGES, String[].class);
                
                return values;
            }

            @Override
            public String[] scanPackages() {
                String[] scanPackages = (String[]) apiVersioningPropertyManager.getValueForProperty(ApiVersioningConfigKeys.DEFAULT_SCAN_PACKAGES, ApiVersioningDefaultConfig.SCAN_PACKAGES, String[].class);
                
                return scanPackages;
            }

            @Override
            public String[] ignorePackages() {
                String[] ignorePackages = (String[]) apiVersioningPropertyManager.getValueForProperty(ApiVersioningConfigKeys.DEFAULT_IGNORE_PACKAGES, ApiVersioningDefaultConfig.IGNORE_PACKAGES, String[].class);
                
                return ignorePackages;
            }

            @Override
            public String[] ignoreClasses() {
                String[] ignoreClasses = (String[]) apiVersioningPropertyManager.getValueForProperty(ApiVersioningConfigKeys.DEFAULT_IGNORE_CLASSES, ApiVersioningDefaultConfig.IGNORE_CLASSES, String[].class);
                
                return ignoreClasses;
            }

            @Override
            public boolean stopAppOnCheckFail() {
                boolean stopAppOnCheckFail = (boolean) apiVersioningPropertyManager.getValueForProperty(ApiVersioningConfigKeys.DEFAULT_STOP_APP_ON_CHECK_FAIL, ApiVersioningDefaultConfig.STOP_APP_ON_CHECK_FAIL, Boolean.class);

                return stopAppOnCheckFail;
            }

        };
        
        // Setting scan packages for AutoApiVersionCheck annotation if empty
        autoApiVersionCheckAnnotation = setScanPackagesForAutoApiVersionCheck(autoApiVersionCheckAnnotation, applicationContext);

        return autoApiVersionCheckAnnotation;
    }

    /**
     * Method to check if controller is annotated with <code>AutoApiVersion<code> annotation.
     * 
     * @param controller                         Controller instance.
     * @return                                   Returns true if controller is annotated else returns false.
     */
    private boolean isApiVersioningAnnotationPresent(Object controller) {

        if (controller.getClass().isAnnotationPresent(ApiVersion.class)) {
            return true;
        }

        return false;
    }

    /**
     * Method to check if controller is skipped for versioning by enabling
     * skipVersioning flag for <code>AutoApiVersion<code> annotation.
     * 
     * @param controller                         Controller instance.
     * @return                                   Returns true if controller is skipped for versioning.
     */
    private boolean isVersioningEnabledForController(Object controller) {
        ApiVersion autoApiVersionAnnotation = controller.getClass().getAnnotation(ApiVersion.class);

        if (autoApiVersionAnnotation.skipVersioning()) {
            versioningSkippedControllers.add(controller.getClass().getName());
            return false;
        }

        return true;
    }

    /**
     * Method to check if controller is ignored for versioning in
     * <code>AutoApiVersionScan<code>
     * 
     * @param controller                         Controller instance.
     * @return                                   Returns true if controller is skipped for versioning.
     */
    private boolean isControllerIgnored(Object controller, ApiVersionCheck autoApiVersionCheckAnnotation) {
        if (null != autoApiVersionCheckAnnotation) {
            List<String> totalIgnoredPackages = new ArrayList<>();
            List<String> totalIgnoredClasses = new ArrayList<>();

            // Packages to be ignored for API versioning.
            String[] ignoredPackages = autoApiVersionCheckAnnotation.ignorePackages();

            // Handling null value for ignorePackages
            if (ignoredPackages == null) {
                ignoredPackages = new String[0];
            }

            // Classes to be ignored for API versioning.
            String[] ignoredClasses = autoApiVersionCheckAnnotation.ignoreClasses();

            // Handling null value for ignoreClasses
            if (ignoredClasses == null) {
                ignoredClasses = new String[0];
            }

            if (ignoredPackages.length != 0) {
                // Check and fetch actual values for annotation params from properties.
                for (int i = 0; i < ignoredPackages.length; i++) {
                    String ignorePackagesString = apiVersioningPropertyManager.getValueForAnnotationParam(autoApiVersionCheckAnnotation, ignoredPackages[i]);

                    if (StringUtils.isNotBlank(ignorePackagesString)) {
                        if (ignorePackagesString.contains(ApiVersioningConstants.COMMA)) {
                            String[] ignorePackagesLoaded = ignorePackagesString.split(ApiVersioningConstants.COMMA);

                            for (int j = 0; j < ignorePackagesLoaded.length; j++) {
                                totalIgnoredPackages.add(ignorePackagesLoaded[j]);
                            }
                        } else {
                            totalIgnoredPackages.add(ignorePackagesString);
                        }
                    }
                }

                // Assigning array with all the packages to be ignored to ignoredPackages array.
                ignoredPackages = totalIgnoredPackages.toArray(new String[totalIgnoredPackages.size()]);
            }

            if (ignoredClasses.length != 0) {
                // Check and fetch actual values for annotation params from properties.
                for (int i = 0; i < ignoredClasses.length; i++) {
                    String ignoredClassesString = apiVersioningPropertyManager.getValueForAnnotationParam(autoApiVersionCheckAnnotation, ignoredClasses[i]);
                    
                    if (StringUtils.isNotBlank(ignoredClassesString)) {
                        if (ignoredClassesString.contains(ApiVersioningConstants.COMMA)) {
                            String [] ignoredClassesLoaded = ignoredClassesString.split(ApiVersioningConstants.COMMA);
                            
                            for (int j = 0; j < ignoredClassesLoaded.length; j++) {
                                totalIgnoredClasses.add(ignoredClassesLoaded[j]);
                            }
                        } else {
                            totalIgnoredClasses.add(ignoredClassesString);
                        }
                    }
                }

                // Assigning array with all the classes to be ignored to ignoredClasses array.
                ignoredClasses = totalIgnoredClasses.toArray(new String[totalIgnoredClasses.size()]);
            }

            boolean isControlledIgnored = false;
            
            // Checking if controller belongs to ignoredClassed in AutoApiVersionCheck
            if (ArrayUtils.contains(ignoredClasses, controller.getClass().getName())) {
                isControlledIgnored = true;
                versioningIgnoredControllersAtClassLevel.add(controller.getClass().getName());
            }
            
            // Checking if controller package belongs to packages/sub-packages
            // mentioned in ignoredPackages in AutoApiVersionCheck.
            if (Boolean.FALSE.equals(isControlledIgnored)) {
                if (ignoredPackages.length != 0) {
                    for (int i = 0; i < ignoredPackages.length; i++) {
                        // Checking if package of controller belongs to package or
                        // sub-package to be ignored.
                        if (controller.getClass().getPackage().getName().contains(ignoredPackages[i])) {
                            isControlledIgnored = true;
                            versioningIgnoredControllersAtPackageLevel.add(controller.getClass().getName());
                        }
                    }
                }
            }

            return isControlledIgnored;
        }

        // Returning false if AutoApiVersionCheck annotation is unavailable.
        return false;
    }

    /**
     * Method to check if controller is available in packages or sub-packeges to be scanned.
     * 
     * @param controller                         Controller instance.
     * @param applicationContext                 Application context.
     * @return                                   Returns true if controller is available in package/sub-package to be scanned.
     *                                           Returns false if controller is not available in package/sub-package to be scanned.
     *                                           Returns true if version check annotation is not available.
     *                                           Returns true if scanPackages param is not mentioned.
     */
    private boolean isControllerBelongsToPackagesToBeScanned(Object controller, ApiVersionCheck autoApiVersionCheckAnnotation) {
        // Packages to be considered for API versioning
        String[] scanPackages = autoApiVersionCheckAnnotation.scanPackages();

        List<String> totalScanPackages = new ArrayList<>();

        // Check and fetch actual values for annotation params from properties.
        for (int i = 0; i < scanPackages.length; i++) {
            String scanPackagesString = apiVersioningPropertyManager.getValueForAnnotationParam(autoApiVersionCheckAnnotation, scanPackages[i]);
            
            if (StringUtils.isNotBlank(scanPackagesString)) {
                if (scanPackagesString.contains(ApiVersioningConstants.COMMA)) {
                    String [] scanPackagesLoaded = scanPackagesString.split(ApiVersioningConstants.COMMA);
                    
                    for (int j = 0; j < scanPackagesLoaded.length; j++) {
                        totalScanPackages.add(scanPackagesLoaded[j]);
                    }
                } else {
                    totalScanPackages.add(scanPackagesString);
                }
            }
        }
        // Assigning array with all the packages to be scanned to scanPackages array.
        scanPackages = totalScanPackages.toArray(new String[totalScanPackages.size()]);
        
        // Checking if version check annotation is given and packages to be
        // scanned are mentioned.
        if (null != autoApiVersionCheckAnnotation && scanPackages.length != 0) {
            for (int i = 0; i < scanPackages.length; i++) {
                // Checking if package of controller belongs to package or
                // sub-package to be scanned.
                if (controller.getClass().getPackage().getName().contains(scanPackages[i])) {
                    return true;
                }
            }

            // Returning false if controller doesn't belong to package or
            // sub-package to be scanned.
            return false;
        }

        // Returning true if version check annotation is not given and packages
        // to be scanned are not mentioned.
        return true;
    }

    /**
     * This method is used to shutdown application when API versioning is
     * missing from classes to be considered for API versioning.
     * 
     */
    private void shutDown(ApplicationContext applicationContext) {
        logger.error("Shutting down the application due to API versioning validation failure.");

        ((ConfigurableApplicationContext) applicationContext).close();
    }

}
