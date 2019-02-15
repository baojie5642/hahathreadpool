package com.baojie.zk.example.watcher.cloud;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A marker interface used as a key in <code>META-INF/spring.factories</code>. Entries in
 * the factories file are used to create the bootstrap application context.
 *
 * @author Dave Syer
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BootstrapConfiguration {

    /**
     * Excludes specific auto-configuration classes such that they will never be applied.
     */
    Class<?>[] exclude() default {};

}
