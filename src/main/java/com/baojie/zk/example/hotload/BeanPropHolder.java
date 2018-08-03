package com.baojie.zk.example.hotload;

import java.lang.reflect.Field;
import java.util.Arrays;

public class BeanPropHolder {
    private final Object bean;
    private final Field field;

    private BeanPropHolder(final Object bean, final Field field) {
        this.bean = bean;
        this.field = field;
    }

    public static BeanPropHolder create(final Object bean, final Field field) {
        if (null == bean || null == field) {
            throw new IllegalStateException("paras null");
        }
        return new BeanPropHolder(bean, field);
    }

    public Object getBean() {
        return this.bean;
    }

    public Field getField() {
        return this.field;
    }

    public int hashCode() {
        return Arrays.hashCode(new Object[]{bean, field});
    }

    public boolean equals(final Object object) {
        if (!(object instanceof BeanPropHolder)) {
            return false;
        } else {
            BeanPropHolder that = (BeanPropHolder) object;
            return compare(getBean(), that.getBean()) && compare(getField(), that.getField());
        }
    }

    private boolean compare(final Object a, final Object b) {
        return a == b || (a != null && a.equals(b));
    }

    @Override
    public String toString() {
        return "BeanPropertyHolder{" +
                "bean=" + bean +
                ", field=" + field +
                '}';
    }

}
