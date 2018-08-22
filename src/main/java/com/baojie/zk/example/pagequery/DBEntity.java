package com.baojie.zk.example.pagequery;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;

@Entity                                        // 标识此类是一个实体
@Table(name = "big_cow_entity")                // 表名称
@Cache(usage = CacheConcurrencyStrategy.NONE)  // 不使用缓存，使用默认模式
@Cacheable(false)                              // 不使用缓存
public class DBEntity {

    private static final Logger log = LoggerFactory.getLogger(DBEntity.class);
    private static final String BLANK = "";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private volatile Long id;                  // 采用id自增长的方式，没有做校验

    @NotBlank(message = "name can not blank")
    private volatile String name;

    public DBEntity() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        String val = name;
        if (null == val) {
            error("name null");
            return BLANK;
        } else {
            return val;
            //return val.trim();
        }
    }

    public void setName(String name) {
        if (null == name) {
            return;
        } else {
            //this.name=name.trim();
            this.name = name;
        }
    }

    private void error(String reason) {
        log.error("error=" + reason + ", info=" + toString());
    }

    @Override
    public String toString() {
        return "DBEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }

}
