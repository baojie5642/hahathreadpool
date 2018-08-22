package com.baojie.zk.example.pagequery;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.internal.util.StringHelper;

import java.util.Locale;

// 使用spring_jpa需要重写hibernate中的类，否则字段匹配出问题
// 这个类要配置在spring_jpa的配置文件中
public class ImprovedNamingStrategy implements PhysicalNamingStrategy {
    public static final ImprovedNamingStrategy INSTANCE = new ImprovedNamingStrategy();

    public ImprovedNamingStrategy() {
    }

    public Identifier toPhysicalCatalogName(Identifier name, JdbcEnvironment context) {
        return name;
    }

    public Identifier toPhysicalSchemaName(Identifier name, JdbcEnvironment context) {
        return name;
    }

    public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment context) {
        String newName = this.addUnderscores(name.getText());
        return Identifier.toIdentifier(newName);
    }

    public Identifier toPhysicalSequenceName(Identifier name, JdbcEnvironment context) {
        return name;
    }

    public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment context) {
        String newName = this.addUnderscores(StringHelper.unqualify(name.getText()));
        return Identifier.toIdentifier(newName);
    }

    protected String addUnderscores(String name) {
        StringBuilder buf = new StringBuilder(name.replace('.', '_'));

        for (int i = 1; i < buf.length() - 1; ++i) {
            if (Character.isLowerCase(buf.charAt(i - 1)) && Character.isUpperCase(
                    buf.charAt(i)) && Character.isLowerCase(buf.charAt(i + 1))) {
                buf.insert(i++, '_');
            }
        }

        return buf.toString().toLowerCase(Locale.ROOT);
    }
}

