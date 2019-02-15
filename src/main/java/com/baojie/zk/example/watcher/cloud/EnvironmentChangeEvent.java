package com.baojie.zk.example.watcher.cloud;

import org.springframework.context.ApplicationEvent;

import java.util.Set;

public class EnvironmentChangeEvent extends ApplicationEvent {

    private Set<String> keys;

    public EnvironmentChangeEvent(Set<String> keys) {
        // Backwards compatible constructor with less utility (practically no use at all)
        this(keys, keys);
    }

    public EnvironmentChangeEvent(Object context, Set<String> keys) {
        super(context);
        this.keys = keys;
    }

    /**
     * @return The keys.
     */
    public Set<String> getKeys() {
        return keys;
    }


}
