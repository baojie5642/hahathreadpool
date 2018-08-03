package com.baojie.zk.example.hotload;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.Properties;

// 继承这个类是为了解决静态调用出错的问题，这个与spring的bean的初始化顺序有关
@Service
public class ResourceHandler extends InstantiationAwareBeanPostProcessorAdapter {

    private final HotLoadResource resource;
    private final HotLoadProcessor processor;

    @Autowired
    public ResourceHandler(HotLoadResource rs, HotLoadProcessor processor) {
        if (null == rs || null == processor) {
            throw new IllegalArgumentException();
        }
        this.resource = rs;
        this.processor = processor;
    }

    @PreDestroy
    private void destory() {

    }

    public Properties getProperties() {
        return resource.getProperties();
    }

    public Resource[] getLocations() {
        return resource.getLocations();
    }

    public HotLoadResource getResource() {
        return resource;
    }

    public HotLoadProcessor getProcessor() {
        return processor;
    }

}
