package com.baojie.zk.example.hotload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Properties;

@Service
public class HotLoadResource extends PropertySourcesPlaceholderConfigurer {
    private static final Logger log = LoggerFactory.getLogger(HotLoadResource.class);
    private volatile Properties properties;
    private volatile Resource[] locations;

    public HotLoadResource() {

    }

    @Override
    protected void loadProperties(final Properties props) {
        superLoad(props);
        this.properties = props;
    }

    private void superLoad(final Properties props) {
        if (null == props) {
            throw new IllegalStateException("props");
        }
        try {
            super.loadProperties(props);
        } catch (IOException e) {
            log.error(e.toString(), e);
        } catch (Throwable t) {
            log.error(t.toString(), t);
        }
    }

    @Override
    public void setLocations(final Resource[] locations) {
        if (null == locations) {
            throw new IllegalStateException("locations");
        }
        superLocation(locations);
        this.locations = locations;
    }

    private void superLocation(final Resource[] locations) {
        super.setLocations(locations);
    }

    protected Properties getProperties() {
        return this.properties;
    }

    protected Resource[] getLocations() {
        return locations;
    }

}
