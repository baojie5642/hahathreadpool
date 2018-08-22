package com.baojie.zk.example.pagequery;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Transactional;

// spring_jpa实现接口即可工作
@Transactional
public interface EntityRepos extends JpaRepository<DBEntity, Long>, JpaSpecificationExecutor<DBEntity> {

}
