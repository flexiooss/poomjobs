package org.codingmatters.poomjobs.service.handlers;

import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerCriteria;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerQuery;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.rest.protocol.CollectionGetProtocol;
import org.codingmatters.poom.services.support.paging.Rfc7233Pager;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;
import org.codingmatters.poomjobs.api.RunnerCollectionGetRequest;
import org.codingmatters.poomjobs.api.RunnerCollectionGetResponse;
import org.codingmatters.poomjobs.api.runnercollectiongetresponse.Status200;
import org.codingmatters.poomjobs.api.runnercollectiongetresponse.Status206;
import org.codingmatters.poomjobs.api.runnercollectiongetresponse.Status416;
import org.codingmatters.poomjobs.api.runnercollectiongetresponse.Status500;
import org.codingmatters.poomjobs.api.types.Error;
import org.codingmatters.poomjobs.api.types.Runner;
import org.codingmatters.poomjobs.service.RunnerEntityTransformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Created by nelt on 7/12/17.
 */
public class RunnerCollectionGetHandler implements CollectionGetProtocol<RunnerValue, RunnerQuery, RunnerCollectionGetRequest, RunnerCollectionGetResponse> {
    static private final Logger log = LoggerFactory.getLogger(RunnerCollectionGetHandler.class);

    private static final int DEFAULT_MAX_PAGE_SIZE = 100;

    private final Repository<RunnerValue, RunnerQuery> repository;
    private final int maxPageSize = DEFAULT_MAX_PAGE_SIZE;

    public RunnerCollectionGetHandler(Repository<RunnerValue, RunnerQuery> repository) {
        this.repository = repository;
    }

    @Override
    public Repository<RunnerValue, RunnerQuery> repository() {
        return this.repository;
    }

    @Override
    public int maxPageSize() {
        return this.maxPageSize;
    }

    @Override
    public String rfc7233Unit() {
        return "Runner";
    }

    @Override
    public String rfc7233Range(RunnerCollectionGetRequest request) {
        return request.range();
    }

    @Override
    public RunnerQuery parseQuery(RunnerCollectionGetRequest request) {
        if(request.nameCompetency() != null || request.categoryCompetency() != null || request.runtimeStatus() != null) {
            Collection<RunnerCriteria> criteria = new LinkedList<>();
            if(request.nameCompetency() != null) {
                criteria.add(RunnerCriteria.builder()
                        .nameCompetency(request.nameCompetency())
                        .build());
            }
            if(request.categoryCompetency() != null) {
                criteria.add(RunnerCriteria.builder()
                        .categoryCompetency(request.categoryCompetency())
                        .build());
            }
            if(request.runtimeStatus() != null) {
                criteria.add(RunnerCriteria.builder()
                        .runtimeStatus(request.runtimeStatus())
                        .build());
            }
            return RunnerQuery.builder().criteria(criteria).build();
        } else {
            return null;
        }
    }

    @Override
    public RunnerCollectionGetResponse partialList(Rfc7233Pager.Page page) {
        return RunnerCollectionGetResponse.builder()
                .status206(Status206.builder()
                        .acceptRange(page.acceptRange())
                        .contentRange(page.contentRange())
                        .payload(this.resultList(page.list()))
                        .build())
                .build();
    }

    @Override
    public RunnerCollectionGetResponse completeList(Rfc7233Pager.Page page) {
        return RunnerCollectionGetResponse.builder()
                .status200(Status200.builder()
                        .acceptRange(page.acceptRange())
                        .contentRange(page.contentRange())
                        .payload(this.resultList(page.list()))
                        .build())
                .build();
    }


    private Collection<Runner> resultList(PagedEntityList<RunnerValue> list) {
        Collection<Runner> result = new LinkedList<>();
        for (Entity<RunnerValue> entity : list) {
            result.add(RunnerEntityTransformation.transform(entity).asRunner());
        }

        return result;
    }

    @Override
    public RunnerCollectionGetResponse invalidRangeQuery(Rfc7233Pager.Page page, String errorToken) {
        log.info(page.validationMessage() + " (requested range: {})", page.requestedRange());
        return RunnerCollectionGetResponse.builder()
                .status416(Status416.builder()
                        .acceptRange(page.acceptRange())
                        .contentRange(page.contentRange())
                        .payload(Error.builder()
                                .code(Error.Code.ILLEGAL_RANGE_SPEC)
                                .description(page.validationMessage())
                                .token(errorToken)
                                .build())
                        .build())
                .build();
    }

    @Override
    public RunnerCollectionGetResponse unexpectedError(RepositoryException e, String errorToken) {
        log.error("unexpected error while handling job list query", e);
        return RunnerCollectionGetResponse.builder()
                .status500(Status500.builder()
                        .payload(Error.builder()
                                .token(errorToken)
                                .code(Error.Code.UNEXPECTED_ERROR)
                                .description("unexpected error, see logs")
                                .build())
                        .build())
                .build();
    }
}
