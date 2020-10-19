package org.codingmatters.poomjobs.service.handlers;

import org.codingmatters.poom.poomjobs.domain.jobs.JobValueCreation;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Accounting;
import org.codingmatters.poom.services.domain.change.Change;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.rest.protocol.CollectionPostProtocol;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poomjobs.api.JobCollectionPostRequest;
import org.codingmatters.poomjobs.api.JobCollectionPostResponse;
import org.codingmatters.poomjobs.api.jobcollectionpostresponse.Status201;
import org.codingmatters.poomjobs.api.jobcollectionpostresponse.Status400;
import org.codingmatters.poomjobs.api.jobcollectionpostresponse.Status500;
import org.codingmatters.poomjobs.api.types.Error;
import org.codingmatters.poomjobs.service.JobValueMerger;
import org.codingmatters.poomjobs.service.PoomjobsJobRepositoryListener;
import org.codingmatters.value.objects.values.ObjectValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 * Created by nelt on 6/15/17.
 */
public class JobCollectionPostHandler implements CollectionPostProtocol<JobValue, JobQuery, JobCollectionPostRequest, JobCollectionPostResponse> {
    static private final Logger log = LoggerFactory.getLogger(JobCollectionPostHandler.class);

    private final Repository<JobValue, JobQuery> repository;
    private final PoomjobsJobRepositoryListener jobRepositoryListener;
    private final Function<JobCollectionPostRequest, ObjectValue> contextualizer;

    public JobCollectionPostHandler(Repository<JobValue, JobQuery> repository, PoomjobsJobRepositoryListener jobRepositoryListener) {
        this(repository, jobRepositoryListener, null);
    }
    public JobCollectionPostHandler(Repository<JobValue, JobQuery> repository, PoomjobsJobRepositoryListener jobRepositoryListener, Function<JobCollectionPostRequest, ObjectValue> contextualizer) {
        this.repository = repository;
        this.jobRepositoryListener = jobRepositoryListener;
        this.contextualizer = contextualizer != null ? contextualizer : request -> ObjectValue.builder().build();
    }

    @Override
    public Logger log() {
        return log;
    }

    @Override
    public Repository<JobValue, JobQuery> repository(JobCollectionPostRequest jobCollectionPostRequest) {
        return this.repository;
    }

    @Override
    public Change<JobValue> valueCreation(JobCollectionPostRequest request) {
        JobValue jobValue = JobValueMerger.create()
                .with(request.payload())
                .withAccounting(Accounting.builder()
                        .accountId(request.accountId())
                        .build())
                .withContext(this.contextualize(request))
                ;

        return JobValueCreation.with(jobValue);
    }

    private ObjectValue contextualize(JobCollectionPostRequest request) {
        return this.contextualizer.apply(request);
    }

    @Override
    public JobCollectionPostResponse entityCreated(JobCollectionPostRequest request, Change<JobValue> creation, Entity<JobValue> entity) {
        this.jobRepositoryListener.jobCreated(entity);
        return JobCollectionPostResponse.builder()
                .status201(Status201.builder()
                        .location("%API_PATH%/jobs/" + entity.id())
                        .build())
                .build();
    }

    @Override
    public JobCollectionPostResponse invalidCreation(Change<JobValue> creation, String errorToken) {
        return JobCollectionPostResponse.builder()
                .status400(Status400.builder()
                        .payload(Error.builder()
                                .token(errorToken)
                                .code(Error.Code.ILLEGAL_JOB_SPEC)
                                .description(creation.validation().message())
                                .build())
                        .build())
                .build();
    }

    @Override
    public JobCollectionPostResponse unexpectedError(Change<JobValue> creation, RepositoryException e, String errorToken) {
        return JobCollectionPostResponse.builder()
                .status500(Status500.builder()
                        .payload(Error.builder()
                                .code(Error.Code.UNEXPECTED_ERROR)
                                .description("unexpected error, see logs")
                                .token(errorToken)
                                .build())
                        .build())
                .build();
    }
}
