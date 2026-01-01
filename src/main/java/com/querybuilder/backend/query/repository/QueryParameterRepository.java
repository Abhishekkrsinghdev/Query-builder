package com.querybuilder.backend.query.repository;

import com.querybuilder.backend.query.model.Query;
import com.querybuilder.backend.query.model.QueryParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QueryParameterRepository extends JpaRepository<QueryParameter, UUID> {

    /**
     * Find all parameters for a query (ordered by display order)
     */
    List<QueryParameter> findByQueryOrderByDisplayOrderAsc(Query query);

    /**
     * Delete all parameters for a query
     */
    void deleteByQuery(Query query);
}