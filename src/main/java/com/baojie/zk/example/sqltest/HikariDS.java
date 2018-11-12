package com.baojie.zk.example.sqltest;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class HikariDS {

    private static final Logger log = LoggerFactory.getLogger(HikariDS.class);

    private final HikariConfig config;
    private final HikariDataSource ds;

    private HikariDS(String user, String passw) {
        if (null == user || null == passw) {
            throw new NullPointerException();
        } else {
            this.config = config(user, passw);
            this.ds = new HikariDataSource(config);
        }
    }

    private HikariConfig config(String user, String passw) {
        final HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost/dataflowdb?characterEncoding=utf8&serverTimezone=Asia/Shanghai");
        config.setUsername(user);
        config.setPassword(passw);
        config.setMaximumPoolSize(50);
        config.setConnectionTimeout(60*1000);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        return config;
    }

    public static HikariDS create(String user, String passw) {
        return new HikariDS(user, passw);
    }

    public Connection getConnect() {
        try {
            return ds.getConnection();
        } catch (SQLException e) {
            log.error(e.toString(), e);
        }
        return null;
    }

    public boolean close(Connection conn) {
        if (null == conn) {
            return false;
        } else {
            try {
                conn.close();
                return true;
            } catch (SQLException e) {
                log.error(e.toString(), e);
            }
            return false;
        }
    }

    public void destory(){
        ds.close();
    }

}
