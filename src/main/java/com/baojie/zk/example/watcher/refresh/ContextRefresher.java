package com.baojie.zk.example.watcher.refresh;

import com.baojie.zk.example.watcher.cloud.BootstrapApplicationListener;
import com.baojie.zk.example.watcher.cloud.EnvironmentChangeEvent;
import com.baojie.zk.example.watcher.cloud.RefreshScope;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.*;

import java.util.*;

public class ContextRefresher {

    private static final String REFRESH_ARGS_PROPERTY_SOURCE = "refreshArgs";

    /**
     * Servlet context init parameters property source name: {@value}.
     */
    public static final String SERVLET_CONTEXT_PROPERTY_SOURCE_NAME = "servletContextInitParams";

    /**
     * Servlet config init parameters property source name: {@value}.
     */
    public static final String SERVLET_CONFIG_PROPERTY_SOURCE_NAME = "servletConfigInitParams";

    /**
     * JNDI property source name: {@value}.
     */
    public static final String JNDI_PROPERTY_SOURCE_NAME = "jndiProperties";

    private static final String[] DEFAULT_PROPERTY_SOURCES = new String[]{
            // order matters, if cli args aren't first, things get messy
            CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME,
            "defaultProperties"};

    private Set<String> standardSources = new HashSet<>(
            Arrays.asList(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME,
                    StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                    JNDI_PROPERTY_SOURCE_NAME,
                    SERVLET_CONFIG_PROPERTY_SOURCE_NAME,
                    SERVLET_CONTEXT_PROPERTY_SOURCE_NAME,
                    "configurationProperties"));

    private final ConfigurableApplicationContext context;
    private final RefreshScope scope;

    public ContextRefresher(ConfigurableApplicationContext context, RefreshScope scope) {
        this.context = context;
        this.scope = scope;
    }

    public ConfigurableApplicationContext getContext() {
        return this.context;
    }

    protected RefreshScope getScope() {
        return this.scope;
    }

    public synchronized Set<String> refresh() {
        Set<String> keys = refreshEnvironment();
        this.scope.refreshAll();
        return keys;
    }

    public synchronized Set<String> refreshEnvironment() {
        Map<String, Object> before = extract(this.context.getEnvironment().getPropertySources());
        addConfigFilesToEnvironment();
        Set<String> keys = changes(before, extract(this.context.getEnvironment().getPropertySources())).keySet();
        this.context.publishEvent(new EnvironmentChangeEvent(context, keys));
        return keys;
    }

    /* For testing. */ ConfigurableApplicationContext addConfigFilesToEnvironment() {
        ConfigurableApplicationContext capture = null;
        try {
            StandardEnvironment environment = copyEnvironment(this.context.getEnvironment());
            SpringApplicationBuilder builder = new SpringApplicationBuilder(Empty.class)
                    .bannerMode(Banner.Mode.OFF)
                    .web(WebApplicationType.NONE)
                    .environment(environment);
            // Just the listeners that affect the environment (e.g. excluding logging
            // listener because it has side effects)
            builder.application().setListeners(
                    Arrays.asList(new BootstrapApplicationListener(), new ConfigFileApplicationListener()));
            capture = builder.run();
            if (environment.getPropertySources().contains(REFRESH_ARGS_PROPERTY_SOURCE)) {
                environment.getPropertySources().remove(REFRESH_ARGS_PROPERTY_SOURCE);
            }
            MutablePropertySources target = this.context.getEnvironment().getPropertySources();
            String targetName = null;
            for (PropertySource<?> source : environment.getPropertySources()) {
                String name = source.getName();
                if (target.contains(name)) {
                    targetName = name;
                }
                if (!this.standardSources.contains(name)) {
                    if (target.contains(name)) {
                        target.replace(name, source);
                    } else {
                        if (targetName != null) {
                            target.addAfter(targetName, source);
                        } else {
                            // targetName was null so we are at the start of the list
                            target.addFirst(source);
                            targetName = name;
                        }
                    }
                }
            }
        } finally {
            ConfigurableApplicationContext closeable = capture;
            while (closeable != null) {
                try {
                    closeable.close();
                } catch (Exception e) {
                    // Ignore;
                }
                if (closeable.getParent() instanceof ConfigurableApplicationContext) {
                    closeable = (ConfigurableApplicationContext) closeable.getParent();
                } else {
                    break;
                }
            }
        }
        return capture;
    }

    // Don't use ConfigurableEnvironment.merge() in case there are clashes with property
    // source names
    private StandardEnvironment copyEnvironment(ConfigurableEnvironment input) {
        StandardEnvironment environment = new StandardEnvironment();
        MutablePropertySources capturedPropertySources = environment.getPropertySources();
        // Only copy the default property source(s) and the profiles over from the main
        // environment (everything else should be pristine, just like it was on startup).
        for (String name : DEFAULT_PROPERTY_SOURCES) {
            if (input.getPropertySources().contains(name)) {
                if (capturedPropertySources.contains(name)) {
                    capturedPropertySources.replace(name,
                            input.getPropertySources().get(name));
                } else {
                    capturedPropertySources.addLast(input.getPropertySources().get(name));
                }
            }
        }
        environment.setActiveProfiles(input.getActiveProfiles());
        environment.setDefaultProfiles(input.getDefaultProfiles());
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("spring.jmx.enabled", false);
        map.put("spring.main.sources", "");
        capturedPropertySources.addFirst(new MapPropertySource(REFRESH_ARGS_PROPERTY_SOURCE, map));
        return environment;
    }

    private Map<String, Object> changes(Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> result = new HashMap<String, Object>();
        for (String key : before.keySet()) {
            if (!after.containsKey(key)) {
                result.put(key, null);
            } else if (!equal(before.get(key), after.get(key))) {
                result.put(key, after.get(key));
            }
        }
        for (String key : after.keySet()) {
            if (!before.containsKey(key)) {
                result.put(key, after.get(key));
            }
        }
        return result;
    }

    private boolean equal(Object one, Object two) {
        if (one == null && two == null) {
            return true;
        }
        if (one == null || two == null) {
            return false;
        }
        return one.equals(two);
    }

    private Map<String, Object> extract(MutablePropertySources propertySources) {
        Map<String, Object> result = new HashMap<String, Object>();
        List<PropertySource<?>> sources = new ArrayList<PropertySource<?>>();
        for (PropertySource<?> source : propertySources) {
            sources.add(0, source);
        }
        for (PropertySource<?> source : sources) {
            if (!this.standardSources.contains(source.getName())) {
                extract(source, result);
            }
        }
        return result;
    }

    private void extract(PropertySource<?> parent, Map<String, Object> result) {
        if (parent instanceof CompositePropertySource) {
            try {
                List<PropertySource<?>> sources = new ArrayList<PropertySource<?>>();
                for (PropertySource<?> source : ((CompositePropertySource) parent)
                        .getPropertySources()) {
                    sources.add(0, source);
                }
                for (PropertySource<?> source : sources) {
                    extract(source, result);
                }
            } catch (Exception e) {
                return;
            }
        } else if (parent instanceof EnumerablePropertySource) {
            for (String key : ((EnumerablePropertySource<?>) parent).getPropertyNames()) {
                result.put(key, parent.getProperty(key));
            }
        }
    }

    @Configuration
    protected static class Empty {

    }

}
