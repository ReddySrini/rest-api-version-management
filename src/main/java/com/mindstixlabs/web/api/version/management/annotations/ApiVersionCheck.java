package com.mindstixlabs.web.api.version.management.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * <code>ApiVersionCheck</code> is an annotation used for enforcing API
 * Versioning in the application.<br>
 * 
 * This annotation accepts attributes for scanning/ignoring packages and
 * classes for API version check validations for application wide ReST
 * APIs.<br>
 * 
 * @author Mindstix Software Labs Pvt. Ltd.
 *         <a href="https://www.mindstix.com">(www.mindstix.com)</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ApiVersionCheck {

    /**
     * Packages to be considered for API versioning enforcement.
     * 
     * @return
     */
    @AliasFor("scanPackages")
    String[] value() default {};

    /**
     * Packages to be considered for API versioning enforcement.
     * 
     * @return
     */
    @AliasFor("value")
    String[] scanPackages() default {};

    /**
     * Packages to be ignored for API versioning enforcement.
     * @return
     */
    String[] ignorePackages() default {};

    /**
     * Classes to be ignored for API versioning enforcement.
     * @return
     */
    String[] ignoreClasses() default {};

    /**
     * Flag to enable/disable API versioning enforcement.
     * @return
     */
    public boolean stopAppOnCheckFail() default true;

}