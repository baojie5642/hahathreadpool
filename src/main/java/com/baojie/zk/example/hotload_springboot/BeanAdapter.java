package com.baojie.zk.example.hotload_springboot;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ComponentScan(basePackages = {"com.baojie"})
public class BeanAdapter extends InstantiationAwareBeanPostProcessorAdapter {

    private static final Map<String, Val> cache = new ConcurrentHashMap<>();

    @Autowired
    public BeanAdapter() {

    }

    @Override
    public boolean postProcessAfterInstantiation(final Object bean, final String beanName) throws BeansException {
        ReflectionUtils.doWithFields(bean.getClass(), ReflectionCallback.create(bean, cache));
        return true;
    }

    public static Map<String,Val> getCache(){
        return cache;
    }

}
