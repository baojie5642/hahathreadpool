package com.baojie.zk.example.hotload_springboot;

import java.lang.reflect.Field;

public class Val {

    private final String hotName;
    private final Field field;
    private final Object bean;

    public Val(String hn, Object bean, Field field) {
        this.hotName = hn;
        this.bean = bean;
        this.field = field;
    }

    public String getHotName() {
        return hotName;
    }

    public Object getBean() {
        return bean;
    }

    public Field getField() {
        return field;
    }

    @Override
    public String toString() {
        return "Val{" +
                "hotName='" + hotName + '\'' +
                ", field=" + field +
                ", bean=" + bean +
                '}';
    }

}
