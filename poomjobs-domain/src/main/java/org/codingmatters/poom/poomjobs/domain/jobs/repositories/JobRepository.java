package org.codingmatters.poom.poomjobs.domain.jobs.repositories;

import com.mongodb.client.MongoClient;
import io.flexio.io.mongo.repository.MongoCollectionRepository;
import org.codingmatters.poom.poomjobs.domain.jobs.repositories.inmemory.InMemoryJobRepository;
import org.codingmatters.poom.poomjobs.domain.jobs.repositories.mongo.MongoJobRepositoryFiltersFilters;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobs.mongo.JobValueMongoMapper;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;

/**
 * Created by nelt on 6/6/17. */
public class JobRepository {
    static public Repository<JobValue, PropertyQuery> createInMemory() {
        return InMemoryRepositoryWithPropertyQuery.notValidating(JobValue.class);
    }

    static public Repository<JobValue, PropertyQuery> createMongo(MongoClient mongoClient, String database) {
        JobValueMongoMapper jobValueMapper = new JobValueMongoMapper();

        return MongoCollectionRepository.<JobValue, JobQuery>repository(database, "jobs")
                .withToDocument(jobValueMapper::toDocument)
                .withToValue(jobValueMapper::toValue)
                .buildWithPropertyQuery(mongoClient);
    }

}
