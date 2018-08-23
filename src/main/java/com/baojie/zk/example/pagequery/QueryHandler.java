package com.baojie.zk.example.pagequery;

import com.baojie.zk.example.hotload.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

public class QueryHandler {

    private static final Logger log = LoggerFactory.getLogger(QueryHandler.class);

    private final EntityRepos repos;

    public QueryHandler(EntityRepos repos){
        if(null==repos){
            throw new NullPointerException();
        }else {
            this.repos=repos;
        }
    }

    public Page<DBEntity> page(int pageNum) {
        SpecialQuery spec = new SpecialQuery();
        Sort sort = new Sort(Sort.Direction.ASC, "id");   // id升序，page中的顺序是从小到大
        int ps= Config.pageSize();
        Pageable p = PageRequest.of(pageNum, ps, sort);
        return query(spec, p);
    }

    private Page<DBEntity> query(Specification<DBEntity> spec, Pageable p) {
        try {
            return repos.findAll(spec, p);
        } catch (Throwable te) {
            log.error(te.toString(), te);
        }
        return null;
    }

}
