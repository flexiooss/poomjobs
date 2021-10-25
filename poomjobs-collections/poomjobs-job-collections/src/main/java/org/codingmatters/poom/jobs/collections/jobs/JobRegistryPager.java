package org.codingmatters.poom.jobs.collections.jobs;

import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.jobs.collections.jobs.repository.JobQueryRewriter;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.EntityLister;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.ImmutableEntity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.service.JobEntityTransformation;

import java.util.stream.Collectors;

public class JobRegistryPager implements PagedCollectionAdapter.Pager<Job>, EntityLister<Job, PropertyQuery> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(JobRegistryPager.class);

    private final String accountId;
    private final JobQuery jobQuery;
    private final EntityLister<JobValue, PropertyQuery> jobValueLister;

    public JobRegistryPager(String accountId, JobQuery jobQuery, EntityLister<JobValue, PropertyQuery> jobValueLister) {
        this.accountId = accountId;
        this.jobQuery = jobQuery;
        this.jobValueLister = jobValueLister;
    }

    @Override
    public String unit() {
        return Job.class.getSimpleName();
    }

    @Override
    public int maxPageSize() {
        return 100;
    }

    @Override
    public EntityLister<Job, PropertyQuery> lister() {
        return this;
    }

    @Override
    public PagedEntityList<Job> all(long startIndex, long endIndex) throws RepositoryException {
        return this.from(this.lookup(this.jobQuery, null, startIndex, endIndex));
    }

    @Override
    public PagedEntityList<Job> search(PropertyQuery query, long startIndex, long endIndex) throws RepositoryException {
        return this.from(this.lookup(this.jobQuery, query, startIndex, endIndex));
    }

    private PagedEntityList<Job> from(PagedEntityList<JobValue> page) {
        return new PagedEntityList.DefaultPagedEntityList<Job>(page.startIndex(), page.endIndex(), page.total(), page.stream().map(entity -> this.from(entity)).collect(Collectors.toList()));
    }

    private Entity<Job> from(Entity<JobValue> entity) {
        return new ImmutableEntity<>(entity.id(), entity.version(), JobEntityTransformation.transform(entity).asJob());
    }

    private PagedEntityList<JobValue> lookup(JobQuery jobQuery, PropertyQuery propertyQuery, long startIndex, long endIndex) throws RepositoryException {
        PropertyQuery query = new JobQueryRewriter().propertyQuery(jobQuery, propertyQuery);
        log.trace("query : {}", query);
        return this.jobValueLister.search(query, startIndex, endIndex);
    }
}
