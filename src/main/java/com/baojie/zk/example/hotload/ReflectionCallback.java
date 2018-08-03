package com.baojie.zk.example.hotload;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class ReflectionCallback implements ReflectionUtils.FieldCallback {
    private static final Logger log = LoggerFactory.getLogger(ReflectionCallback.class);
    private final SimpleTypeConverter typeConverter = new SimpleTypeConverter();
    private final HotLoadProcessor processor;
    private final Object bean;

    private ReflectionCallback(Object bean, HotLoadProcessor processor) {
        if (null == bean || null == processor) {
            throw new IllegalArgumentException();
        }
        this.bean = bean;
        this.processor = processor;
    }

    public static ReflectionCallback create(Object bean, HotLoadProcessor p) {
        return new ReflectionCallback(bean, p);
    }

    @Override
    public void doWith(final Field field) {
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
        modifyCheck(field);
        HotReloadable h = (HotReloadable) a;
        String k = keyString(h);
        if (StringUtils.isBlank(k)) {
            throw new IllegalArgumentException("annotation value null");
        }
        Object pv = processor.getResource().getProperties().get(k);
        validate(k, pv, field);
        if (null == pv) {
            return;
        }
        setPropFromConfig(pv, field);
        BeanPropHolder bh = BeanPropHolder.create(bean, field);
        processor.getBeanProp().putIfAbsent(k, bh);
    }

    private void modifyCheck(final Field field) {
        if (!Modifier.isStatic(field.getModifiers())) {

        }
        // 通过反射来做，final字段的值也是可以用来改变的，这里不这样做
        if (Modifier.isFinal(field.getModifiers())) {
            throw new IllegalArgumentException("final field, name=" + field.getClass() + "." + field.getName());
        }
    }

    private String keyString(final HotReloadable h) {
        final String temp = h.value();
        if (StringUtils.isBlank(temp)) {
            throw new IllegalArgumentException("value blank");
        } else {
            return temp.replace("${", "").replace("}", "");
        }
    }

    private void validate(final String k, final Object p, final Field field) {
        Object o = null;
        try {
            o = field.get(bean);   // 修复潜在的一个bug
        } catch (IllegalAccessException e) {
            log.error(e.toString() + ", k=" + k, e);
        } catch (Throwable t) {
            log.error(t.toString() + ", k=" + k, t);
        }
        // 如果properties配置文件中找不到，并且在代码中还没有默认值，那么会报错，启动异常
        if (null == p && null == o) {
            throw new IllegalArgumentException("properties null and default value null");
        }
    }

    private void setPropFromConfig(final Object pv, final Field field) {
        Object propRealValue = typeConverter.convertIfNecessary(pv, field.getType());
        try {
            field.set(bean, propRealValue);
        } catch (IllegalAccessException e) {
            log.error(e.toString(), e);
        } catch (Throwable t) {
            log.error(t.toString(), t);
        }
    }

    @Override
    public String toString() {
        return "ReflectionCallback{" +
                "typeConverter=" + typeConverter +
                ", processor=" + processor +
                ", bean=" + bean +
                '}';
    }
}
