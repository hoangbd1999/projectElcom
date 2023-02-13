package com.elcom.metacen.group.detect.repository.mongo.impl;

import com.elcom.metacen.group.detect.repository.mongo.BaseCustomRepository;
import com.mongodb.client.result.UpdateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.List;

/**
 *
 * @author Admin
 */
@NoRepositoryBean
public class BaseCustomMongoRepositoryImpl<E extends Serializable> implements BaseCustomRepository<E> {


    @Autowired
    protected MongoOperations mongoOps;

    @Autowired
    @Qualifier("mongoContactTemplate")
    protected MongoOperations mongoContactOps;

    protected Class<E> domain;

    public BaseCustomMongoRepositoryImpl() {
        this.domain = (Class<E>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        //this.pipeline = QueryConversionPipeline.defaultPipeline();
//        this.pipeline = QueryConversionPipeline
//                .builder()
//                .useNonDefaultArgumentConversionPipe(EnhancedArgumentConversionPipe
//                        .builder()
//                        .useNonDefaultFieldResolver(new EnhancedEntityFieldTypeResolver())
//                        .useNonDefaultStringToTypeConverter(new NewSpringConversionServiceConverter())
//                        .build()).build();
    }

//    @Override
//    public Criteria getRSQLCriteria(String rsql, Class domain) {
//        Condition<GeneralQueryBuilder> condition = pipeline.apply(rsql, domain);
//        Criteria criteria = condition.query(new MongoVisitor());
//
//        return criteria;
//    }

//    @Override
//    public List<E> searchAllByRsql(String rsql) {
//        Criteria criteria = getRSQLCriteria(rsql, domain);
//        Query query = new Query().addCriteria(criteria);
//
//        List<E> list = mongoOps.find(query, domain);
//
//        return list;
//    }
//
//    @Override
//    public Page<E> searchByRsql(String rsql, Pageable pageable) {
//        Criteria criteria = getRSQLCriteria(rsql, domain);
//        Query query = new Query().with(pageable).addCriteria(criteria);
//
//        List<E> list = mongoOps.find(query, domain);
//        Page<E> page = new PageImpl<>(list, pageable, mongoOps.count(Query.query(criteria).limit(-1).skip(-1), domain));
//
//        return page;
//    }

    @Override
    public Page<E> searchAllByCriteria(Criteria criteria, Pageable pageable) {
        Query query = new Query().with(pageable).addCriteria(criteria);
        query.with(Sort.by(Sort.Direction.ASC, "name"));

        List<E> list = mongoOps.find(query, domain);
        Page<E> page = new PageImpl<>(list, pageable, mongoOps.count(Query.query(criteria).limit(-1).skip(-1), domain));

        return page;
    }

    @Override
    public UpdateResult updateFirst(Query query, Update update) {
        return mongoOps.updateFirst(query, update, domain);
    }

    @Override
    public UpdateResult updateMulti(Query query, Update update) {
        return mongoOps.updateMulti(query, update, domain);
    }
}
