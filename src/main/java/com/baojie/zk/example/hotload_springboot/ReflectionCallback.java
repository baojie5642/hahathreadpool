package com.baojie.zk.example.hotload_springboot;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

public final class ReflectionCallback implements ReflectionUtils.FieldCallback {
    private static final Logger log = LoggerFactory.getLogger(ReflectionCallback.class);
    private final SimpleTypeConverter typeConverter = new SimpleTypeConverter();
    private final Map<String, Val> cache;
    private final Object bean;

    private ReflectionCallback(Object bean, Map<String, Val> cache) {
        if (null == bean || null == cache) {
            throw new IllegalArgumentException();
        } else {
            this.bean = bean;
            this.cache = cache;
        }
    }

    public static ReflectionCallback create(Object bean, Map<String, Val> cache) {
        return new ReflectionCallback(bean, cache);
    }

    @Override
    public void doWith(Field field) {
        if (null == field) {
            return;
        }
        ReflectionUtils.makeAccessible(field);
        Object a = field.getAnnotation(HotReloadable.class);
        if (null == a) {
            return;
        }
        if (!(a instanceof HotReloadable)) {
            return;
        }
        check(field);
        HotReloadable h = (HotReloadable) a;
        String k = key(h);
        if (StringUtils.isBlank(k)) {
            throw new IllegalArgumentException("annotation value null");
        }
        Object v = value(field);
        System.out.println(v);
        final Val val = new Val(k, bean, field);
        cache.putIfAbsent(k, val);
    }

    private void check(Field field) {
        // 通过反射来做，final字段的值也是可以用来改变的，这里不这样做
        if (Modifier.isFinal(field.getModifiers())) {
            throw new IllegalArgumentException("final field, name=" + field.getClass() + "." + field.getName());
        }
    }

    private String key(HotReloadable h) {
        final String temp = h.value();
        if (StringUtils.isBlank(temp)) {
            throw new IllegalArgumentException("value blank");
        }
        return temp.replace("${", "").replace("}", "");
    }

    private Object value(Field field) {
        try {
            return field.get(bean);
        } catch (IllegalAccessException e) {
            log.error(e.toString(), e);
        } catch (Throwable t) {
            log.error(t.toString(), t);
        }
        return null;
    }

    @Override
    public String toString() {
        return "ReflectionCallback{" +
                "typeConverter=" + typeConverter +
                ", bean=" + bean +
                '}';
    }

}
