package org.codingmatters.poom.poomjobs.domain.jobs.repositories.mongo;

import io.flexio.services.support.mondo.MongoFilterBuilder;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.conversions.Bson;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobCriteria;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.jobs.ValueList;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerCriteria;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerQuery;

import java.util.LinkedList;
import java.util.List;

public class MongoJobRepositoryFiltersFilters {

    public Bson filterJobValues(JobQuery jobQuery) {
        List<Bson> filters = new LinkedList<>();
        if(jobQuery.opt().criteria().isPresent()) {
            for (JobCriteria jobCriteria : jobQuery.criteria()) {
                filters.add(this.criteriaFilter(jobCriteria));
            }
        }

        Bson compound = this.all(filters);
        return compound;
    }

    public Bson filterRunnerValues(RunnerQuery runnerQuery) {
        MongoFilterBuilder builder = MongoFilterBuilder.filter();
        if(runnerQuery.opt().criteria().isPresent()) {
            for (RunnerCriteria runnerCriteria : runnerQuery.criteria()) {
                builder.having(runnerCriteria.opt().categoryCompetency(), competency ->
                        com.mongodb.client.model.Filters.elemMatch("competencies.categories", new BsonDocument("$eq", new BsonString(competency)))
                );
                builder.having(runnerCriteria.opt().nameCompetency(), competency ->
                        com.mongodb.client.model.Filters.elemMatch("competencies.names", new BsonDocument("$eq", new BsonString(competency)))
                );
                builder.having(runnerCriteria.opt().runtimeStatus(), status ->
                        com.mongodb.client.model.Filters.eq("runtime.status", status));
            }

        }
        return builder.build();
    }

    private Bson criteriaFilter(JobCriteria criteria) {
        List<Bson> filters = new LinkedList<>();
        if(criteria.opt().category().isPresent()) {
            filters.add(com.mongodb.client.model.Filters.eq("category", criteria.category()));
        }
        if(criteria.opt().runStatus().isPresent()) {
            filters.add(com.mongodb.client.model.Filters.eq("status.run", criteria.runStatus()));
        }
        if(criteria.opt().exitStatus().isPresent()) {
            filters.add(com.mongodb.client.model.Filters.eq("status.exit", criteria.exitStatus()));
        }
        if(criteria.opt().names().isPresent() && ! criteria.names().isEmpty()) {
            filters.add(this.namesFilter(criteria.names()));
        }

        return this.all(filters);
    }

    private Bson namesFilter(ValueList<String> names) {
        List<Bson> filters = new LinkedList<>();
        for (String name : names) {
            filters.add(com.mongodb.client.model.Filters.regex("name", name));
        }

        return this.one(filters);
    }

    private Bson all(List<Bson> filters) {
        if(filters.isEmpty()) {
            return null;
        }
        if(filters.size() == 1) {
            return filters.get(0);
        }
        return com.mongodb.client.model.Filters.and(filters);
    }

    private Bson one(List<Bson> filters) {
        if(filters.isEmpty()) {
            return null;
        }
        if(filters.size() == 1) {
            return filters.get(0);
        }
        return com.mongodb.client.model.Filters.or(filters);
    }
}
