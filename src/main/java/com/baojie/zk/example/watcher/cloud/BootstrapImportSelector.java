package com.baojie.zk.example.watcher.cloud;

import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.style.ToStringCreator;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BootstrapImportSelector implements EnvironmentAware, DeferredImportSelector {

    private Environment environment;

    private MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        // Use names and ensure unique to protect against duplicates
        List<String> names = new ArrayList<>(SpringFactoriesLoader
                .loadFactoryNames(BootstrapConfiguration.class, classLoader));
        names.addAll(Arrays.asList(StringUtils.commaDelimitedListToStringArray(
                environment.getProperty("spring.cloud.bootstrap.sources", ""))));

        List<OrderedAnnotatedElement> elements = new ArrayList<>();
        for (String name : names) {
            try {
                elements.add(new OrderedAnnotatedElement(metadataReaderFactory, name));
            } catch (IOException e) {
                continue;
            }
        }
        AnnotationAwareOrderComparator.sort(elements);

        String[] classNames = elements.stream()
                .map(e -> e.name)
                .toArray(String[]::new);

        return classNames;
    }

    class OrderedAnnotatedElement implements AnnotatedElement {

        private final String name;
        private Order order = null;
        private Integer value;

        public OrderedAnnotatedElement(MetadataReaderFactory metadataReaderFactory, String name) throws IOException {
            MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(name);
            AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
            Map<String, Object> attributes = metadata.getAnnotationAttributes(Order.class.getName());
            this.name = name;
            if (attributes != null && attributes.containsKey("value")) {
                value = (Integer) attributes.get("value");
                order = new Order() {
                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return Order.class;
                    }

                    @Override
                    public int value() {
                        return value;
                    }
                };
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            if (annotationClass == Order.class) {
                return (T) order;
            }
            return null;
        }

        @Override
        public Annotation[] getAnnotations() {
            return order == null ? new Annotation[0] : new Annotation[]{order};
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return getAnnotations();
        }

        @Override
        public String toString() {
            return new ToStringCreator(this)
                    .append("name", name)
                    .append("value", value)
                    .toString();
        }
    }
}
