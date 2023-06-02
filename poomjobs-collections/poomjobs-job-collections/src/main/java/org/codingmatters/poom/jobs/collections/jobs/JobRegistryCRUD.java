package org.codingmatters.poom.jobs.collections.jobs;

import org.codingmatters.poom.api.paged.collection.api.types.Error;
import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.poomjobs.domain.jobs.JobValueChange;
import org.codingmatters.poom.poomjobs.domain.jobs.JobValueCreation;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Accounting;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.poom.services.domain.entities.ImmutableEntity;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.JobCreationData;
import org.codingmatters.poomjobs.api.types.JobUpdateData;
import org.codingmatters.poomjobs.service.JobEntityTransformation;
import org.codingmatters.poomjobs.service.JobValueMerger;
import org.codingmatters.poomjobs.service.PoomjobsJobRepositoryListener;
import org.codingmatters.value.objects.values.ObjectValue;

import java.math.BigInteger;
import java.util.Optional;

import static org.codingmatters.poomjobs.service.JobValueMerger.merge;

public class JobRegistryCRUD implements PagedCollectionAdapter.CRUD<Job, JobCreationData, Void, JobUpdateData> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(JobRegistryCRUD.class);

    private final Repository<JobValue, PropertyQuery> repository;
    private final String url;
    private final String accountId;
    private final String xExtension;
    private final ObjectValue context;
    private final BigInteger fromVersion;
    private final PoomjobsJobRepositoryListener listener;

    public JobRegistryCRUD(Repository<JobValue, PropertyQuery> repository, String url, String accountId, String xExtension, ObjectValue context, BigInteger fromVersion, PoomjobsJobRepositoryListener listener) {
        this.repository = repository;
        this.url = url;
        this.accountId = accountId;
        this.xExtension = xExtension;
        this.context = context;
        this.fromVersion = fromVersion;
        this.listener = listener;
    }

    @Override
    public String entityType() {
        return Job.class.getSimpleName();
    }

    @Override
    public String entityRepositoryUrl() {
        return this.url;
    }


    @Override
    public Entity<Job> createEntityFrom(JobCreationData jobCreationData) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException, MethodNotAllowedException {
        JobValue jobValue = JobValueMerger.create()
                .with(jobCreationData)
                .withAccounting(Accounting.builder()
                        .accountId(this.accountId)
                        .extension(this.xExtension)
                        .build())
                .withContext(this.context)
                ;
        JobValueCreation creation = JobValueCreation.with(jobValue);
        if(creation.validation().isValid()) {
            Entity<JobValue> created;
            try {
                created = this.repository.create(creation.applied());
            } catch (RepositoryException e) {
                throw this.unexpectedException("error creating job", e);
            }
            listener.jobCreated(created);
            return new ImmutableEntity<>(created.id(), created.version(), JobEntityTransformation.transform(created).asJob());
        } else {
            throw new BadRequestException(Error.builder()
                    .code(Error.Code.INVALID_OBJECT_FOR_CREATION)
                    .token(log.tokenized().info("invalid for job creation : {} - {}", creation.validation().message(), jobCreationData))
                    .description(creation.validation().message())
                    .build(), creation.validation().message());
        }
    }

    @Override
    public Optional<Entity<Job>> retrieveEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException, MethodNotAllowedException {
        Entity<JobValue> entity;
        try {
            entity = this.repository.retrieve(id);
        } catch (RepositoryException e) {
            throw this.unexpectedException("cannot retrieve job", e);
        }
        if(entity != null) {
            return Optional.of(
                    new ImmutableEntity<>(entity.id(), entity.version(), JobEntityTransformation.transform(entity).asJob())
            );
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Entity<Job> updateEntityWith(String id, JobUpdateData jobUpdateData) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException, MethodNotAllowedException {
        log.debug("job update request...");
        Entity<JobValue> entity;
        try {
            entity = this.repository.retrieve(id);
            log.debug("job update request... retrieved {}", entity);
            if(entity == null) {
                throw new NotFoundException(Error.builder()
                        .code(Error.Code.RESOURCE_NOT_FOUND)
                        .token(log.tokenized().info("while updating, job not found : {}", id))
                        .build(), "while updating, job not found");
            }
        } catch (RepositoryException e) {
            throw this.unexpectedException("while updating, cannot retrieve job", e);
        }
        JobValue newValue = merge(entity.value()).with(jobUpdateData);
        JobValueChange change = JobValueChange.from(entity.version(), this.fromVersion, entity.value()).to(newValue);
        if(change.validation().isValid()) {
            Entity<JobValue> updated;
            try {
                updated = this.repository.update(entity, change.applied());
            } catch (RepositoryException e) {
                throw this.unexpectedException("error updating job", e);
            }
            listener.jobUpdated(updated);
            return new ImmutableEntity<>(updated.id(), updated.version(), JobEntityTransformation.transform(updated).asJob());
        } else {
            throw new BadRequestException(Error.builder()
                    .code(Error.Code.INVALID_OBJECT_FOR_UPDATE)
                    .token(log.tokenized().info("invalid for job {} update : {} - {}", entity.id(), change.validation().message(), jobUpdateData))
                    .description(change.validation().message())
                    .build(), change.validation().message());
        }
    }

    @Override
    public void deleteEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException, MethodNotAllowedException {
        Entity<JobValue> entity;
        try {
            entity = this.repository.retrieve(id);
        } catch (RepositoryException e) {
            throw this.unexpectedException("while deleting job, failed looking up " + id, e);
        }
        if(entity != null) {
            try {
                this.repository.delete(entity);
            } catch (RepositoryException e) {
                throw this.unexpectedException("error deleting job " + id, e);
            }
        } else {
            throw new NotFoundException(Error.builder()
                    .code(Error.Code.RESOURCE_NOT_FOUND)
                    .token(log.tokenized().info("no job with id : ", id))
                    .build(),
                    "job not found");
        }
    }

    private UnexpectedException unexpectedException(String message, RepositoryException e) {
        return new UnexpectedException(
                Error.builder()
                        .code(Error.Code.UNEXPECTED_ERROR)
                        .token(log.tokenized().error(message + ": error with job repository", e))
                        .build(),
                message
        );
    }

    @Override
    public Entity<Job> replaceEntityWith(String s, Void unused) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException, MethodNotAllowedException {
        throw new MethodNotAllowedException(Error.builder()
                .code(Error.Code.ENTITY_REPLACEMENT_NOT_ALLOWED)
                .build(), "replace job not allowed");
    }

}
