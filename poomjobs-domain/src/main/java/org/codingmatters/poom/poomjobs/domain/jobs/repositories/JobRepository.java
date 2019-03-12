package org.codingmatters.poom.poomjobs.domain.jobs.repositories;

import com.mongodb.MongoClient;
import io.flexio.io.mongo.repository.MongoCollectionRepository;
import org.codingmatters.poom.poomjobs.domain.jobs.repositories.inmemory.InMemoryJobRepository;
import org.codingmatters.poom.poomjobs.domain.jobs.repositories.mongo.MongoJobRepositoryFiltersFilters;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobs.mongo.JobValueMongoMapper;
import org.codingmatters.poom.services.domain.repositories.Repository;

/**
 * Created by nelt on 6/6/17.
 */
public class JobRepository {
    static public Repository<JobValue, JobQuery> createInMemory() {
        return new InMemoryJobRepository();
    }

    static public Repository<JobValue, JobQuery> createMongo(MongoClient mongoClient, String database) {
        JobValueMongoMapper jobValueMapper = new JobValueMongoMapper();
        MongoJobRepositoryFiltersFilters filters = new MongoJobRepositoryFiltersFilters();

        return MongoCollectionRepository.<JobValue, JobQuery>repository(database, "jobs")
                .withToDocument(jobValueMapper::toDocument)
                .withToValue(jobValueMapper::toValue)
                .withFilter(filters::filterJobValues)
                .build(mongoClient);
    }

}
