package org.codingmatters.poom.jobs.collections.jobs;

import org.codingmatters.poom.poomjobs.domain.values.jobs.JobCriteria;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poomjobs.api.JobCollectionGetRequest;
import org.codingmatters.poomjobs.api.JobCollectionPostRequest;
import org.codingmatters.poomjobs.api.JobResourcePatchRequest;
import org.codingmatters.poomjobs.api.PoomjobsJobRegistryAPIHandlers;
import org.codingmatters.poomjobs.api.collection.handlers.JobCollectionBrowse;
import org.codingmatters.poomjobs.api.collection.handlers.JobCollectionCreate;
import org.codingmatters.poomjobs.api.collection.handlers.JobCollectionRetrieve;
import org.codingmatters.poomjobs.api.collection.handlers.JobCollectionUpdate;
import org.codingmatters.poomjobs.service.PoomjobsJobRepositoryListener;
import org.codingmatters.value.objects.values.ObjectValue;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

public class JobRegistryHandlersBuilder extends PoomjobsJobRegistryAPIHandlers.Builder {

    public JobRegistryHandlersBuilder(
            Repository<JobValue, PropertyQuery> jobValueRepository,
            String url,
            Function<JobCollectionPostRequest, ObjectValue> contextualizer,
            PoomjobsJobRepositoryListener jobRepositoryListener
    ) {
        Function<JobCollectionPostRequest, ObjectValue> ctxizer = contextualizer == null ? r -> null : contextualizer;

        this.jobCollectionGetHandler(new JobCollectionBrowse(request -> new JobRegistryPager(request.accountId(), this.parseQuery(request), jobValueRepository)));
        this.jobCollectionPostHandler(new JobCollectionCreate(request -> new JobRegistryCRUD(jobValueRepository, url, request.accountId(), request.xExtension(), ctxizer.apply(request), null, jobRepositoryListener)));
        this.jobResourceGetHandler(new JobCollectionRetrieve(request -> new JobRegistryCRUD(jobValueRepository, url, request.accountId(), null, null, null, jobRepositoryListener)));
        this.jobResourcePatchHandler(new JobCollectionUpdate(request -> new JobRegistryCRUD(jobValueRepository, url, request.accountId(), null, null, this.fromVersion(request), jobRepositoryListener)));
    }

    public JobQuery parseQuery(JobCollectionGetRequest request) {
        JobQuery query = null;
        if(request.opt().names().isPresent() || request.opt().category().isPresent() || request.opt().runStatus().isPresent() || request.opt().exitStatus().isPresent()) {
            List<JobCriteria> criteria = new LinkedList<>();
            if(request.opt().names().isPresent()) {
                criteria.add(JobCriteria.builder().names(request.names().toArray(new String[request.names().size()])).build());
            }
            if(request.opt().category().isPresent()) {
                criteria.add(JobCriteria.builder().category(request.category()).build());
            }
            if(request.opt().runStatus().isPresent()) {
                criteria.add(JobCriteria.builder().runStatus(request.runStatus()).build());
            }
            if(request.opt().exitStatus().isPresent()) {
                criteria.add(JobCriteria.builder().exitStatus(request.exitStatus()).build());
            }
            query = JobQuery.builder().criteria(criteria).build();
        }
        return query;
    }

    private BigInteger fromVersion(JobResourcePatchRequest request) {
        if(request.opt().strict().isPresent() && request.strict()) {
            if(request.opt().currentVersion().isPresent()) {
                return new BigInteger(request.currentVersion());
            } else {
                return BigInteger.ONE;
            }
        } else {
            return null;
        }
    }
}
