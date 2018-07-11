package com.mindstixlabs.web.api.version.management.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <code>ApiVersion</code> is an annotation used for Auto API versioning for
 * Rest APIs. This annotation accepts numeric version for controller APIs and
 * flag to skip API Versioning for controller APIs.
 * 
 * @author Mindstix Software Labs Pvt. Ltd.
 *         <a href="https://www.mindstix.com">(www.mindstix.com)</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ApiVersion {

    /**
     * API Version to be applied for all APIs in the controller
     * @return
     */
    public String value() default "";

    /**
     * Flag to enable/disable versioning for all APIs in the controller.
     * @return
     */
    public boolean skipVersioning() default false;

}