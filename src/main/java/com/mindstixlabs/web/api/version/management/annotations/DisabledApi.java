package com.mindstixlabs.web.api.version.management.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.mindstixlabs.web.api.version.management.ReSTApiVersionManager;

/**
 * <code>DisabledApi</code> is an annotation used for disabling particular API
 * version.<br>
 * 
 * The API Versioning context will be applied to controllers or methods
 * with <code>DisabledApi</code> annotation but the disabled APIs will not be
 * invoked by {@link ReSTApiVersionManager} through direct requests or
 * fallback as per configuration.
 * 
 * @author Mindstix Software Labs Pvt. Ltd.
 *         <a href="https://www.mindstix.com">(www.mindstix.com)</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.TYPE, ElementType.METHOD })
public @interface DisabledApi {

}