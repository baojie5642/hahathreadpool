package com.baojie.zk.example.pagequery;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
public class QueryService {

    private final QueryHandler handler;

    @Autowired
    public QueryService(EntityRepos repos) {
        if (null == repos) {
            throw new NullPointerException();
        } else {
            this.handler = new QueryHandler(repos);
        }
    }

    public Page<DBEntity> query(int pageNum) {
        return handler.page(pageNum);
    }

}
