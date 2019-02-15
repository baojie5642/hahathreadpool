package com.baojie.zk.example.watcher.cloud;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

@Service
@ManagedResource
public class RefreshScope extends GenericScope implements ApplicationContextAware, ApplicationListener<ContextRefreshedEvent>, Ordered {

    private ApplicationContext context;
    private BeanDefinitionRegistry registry;
    private boolean eager = true;
    private int order = Ordered.LOWEST_PRECEDENCE - 100;

    /**
     * Creates a scope instance and gives it the default name: "refresh".
     */
    public RefreshScope() {
        super.setName("refresh");
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    /**
     * Flag to determine whether all beans in refresh scope should be instantiated eagerly
     * on startup. Default true.
     *
     * @param eager The flag to set.
     */
    public void setEager(boolean eager) {
        this.eager = eager;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        this.registry = registry;
        super.postProcessBeanDefinitionRegistry(registry);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        start(event);
    }

    public void start(ContextRefreshedEvent event) {
        if (event.getApplicationContext() == this.context && this.eager && this.registry != null) {
            eagerlyInitialize();
        }
    }

    private void eagerlyInitialize() {
        for (String name : this.context.getBeanDefinitionNames()) {
            BeanDefinition definition = this.registry.getBeanDefinition(name);
            if (this.getName().equals(definition.getScope())
                    && !definition.isLazyInit()) {
                Object bean = this.context.getBean(name);
                if (bean != null) {
                    bean.getClass();
                }
            }
        }
    }

    @ManagedOperation(description = "Dispose of the current instance of bean name provided and force a refresh on next method execution.")
    public boolean refresh(String name) {
        if (!name.startsWith(SCOPED_TARGET_PREFIX)) {
            // User wants to refresh the bean with this name but that isn't the one in the
            // cache...
            name = SCOPED_TARGET_PREFIX + name;
        }
        // Ensure lifecycle is finished if bean was disposable
        if (super.destroy(name)) {
            this.context.publishEvent(new RefreshScopeRefreshedEvent(name));
            return true;
        }
        return false;
    }

    @ManagedOperation(description = "Dispose of the current instance of all beans in this scope and force a refresh on next method execution.")
    public void refreshAll() {
        super.destroy();
        this.context.publishEvent(new RefreshScopeRefreshedEvent());
    }

    // service已经进行了set操作
    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }

}
