package org.codingmatters.tasks.support.api.repos.mongo;

import com.mongodb.client.MongoClient;
import io.flexio.io.mongo.repository.MongoCollectionRepository;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.tasks.api.types.Task;
import org.codingmatters.tasks.api.types.TaskLog;
import org.codingmatters.tasks.api.types.mongo.TaskLogMongoMapper;
import org.codingmatters.tasks.api.types.mongo.TaskMongoMapper;

public class MongoTaskRepositories {
    private final MongoClient mongoClient;
    private final String db;
    private final String task;

    public MongoTaskRepositories(MongoClient mongoClient, String db, String task) {
        this.mongoClient = mongoClient;
        this.db = db;
        this.task = task;
    }

    public Repository<Task, PropertyQuery> tasks() {
        TaskMongoMapper mapper = new TaskMongoMapper();
        return MongoCollectionRepository.<Task, PropertyQuery>repository(this.db, this.task + "_tasks")
                .withToDocument(mapper::toDocument)
                .withToValue(mapper::toValue)
                .buildWithPropertyQuery(this.mongoClient);
    }

    public Repository<TaskLog, PropertyQuery> taskLogs() {
        TaskLogMongoMapper mapper = new TaskLogMongoMapper();
        return MongoCollectionRepository.<TaskLog, PropertyQuery>repository(this.db, this.task + "_task_logs")
                .withToDocument(mapper::toDocument)
                .withToValue(mapper::toValue)
                .buildWithPropertyQuery(this.mongoClient);
    }
}
