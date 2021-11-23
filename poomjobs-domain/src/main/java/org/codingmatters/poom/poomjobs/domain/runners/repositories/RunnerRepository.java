package org.codingmatters.poom.poomjobs.domain.runners.repositories;

import com.mongodb.client.MongoClient;
import io.flexio.io.mongo.repository.MongoCollectionRepository;
import org.codingmatters.poom.poomjobs.domain.jobs.repositories.mongo.MongoJobRepositoryFiltersFilters;
import org.codingmatters.poom.poomjobs.domain.runners.repositories.inmemory.InMemoryRunnerRepository;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerQuery;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.poomjobs.domain.values.runners.mongo.RunnerValueMongoMapper;
import org.codingmatters.poom.services.domain.repositories.Repository;

/**
 * Created by nelt on 7/10/17.
 */
public class RunnerRepository {
    static public Repository<RunnerValue, RunnerQuery> createInMemory() {
        return new InMemoryRunnerRepository();
    }

    static public Repository<RunnerValue, RunnerQuery> createMongo(MongoClient mongoClient, String database) {
        RunnerValueMongoMapper runnerValueMapper = new RunnerValueMongoMapper();
        MongoJobRepositoryFiltersFilters filters = new MongoJobRepositoryFiltersFilters();

        return MongoCollectionRepository.<RunnerValue, RunnerQuery>repository(database, "runners")
                .withToDocument(runnerValueMapper::toDocument)
                .withToValue(runnerValueMapper::toValue)
                .withFilter(filters::filterRunnerValues)
                .build(mongoClient);
    }
}
