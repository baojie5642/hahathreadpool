package com.baojie.zk.example.pagequery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

public class SpecialQuery implements Specification<DBEntity> {

    private static final Logger log = LoggerFactory.getLogger(SpecialQuery.class);

    public SpecialQuery() {

    }

    @Override
    public Predicate toPredicate(Root<DBEntity> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
        return null;
    }

}
