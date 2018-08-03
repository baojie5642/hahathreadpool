package com.baojie.zk.example.hotload;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class HotLoadProcessor extends InstantiationAwareBeanPostProcessorAdapter {
    private final Map<String, BeanPropHolder> beanMap = new ConcurrentHashMap<>(256);
    private final HotLoadResource resource;

    @Autowired
    public HotLoadProcessor(HotLoadResource resource) {
        if (null == resource) {
            throw new IllegalStateException("hotLoadResource");
        }
        this.resource = resource;
    }

    @PreDestroy
    private void destory() {
        beanMap.clear();
    }

    @Override
    public boolean postProcessAfterInstantiation(final Object bean, final String beanName) throws BeansException {
        ReflectionUtils.doWithFields(bean.getClass(), ReflectionCallback.create(bean, this));
        return true;
    }

    public Map<String, BeanPropHolder> getBeanProp() {
        return beanMap;
    }

    public HotLoadResource getResource() {
        return resource;
    }

}
