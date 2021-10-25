package org.codingmatters.poom.jobs.collections.jobs.repository;

import org.codingmatters.poom.poomjobs.domain.values.jobs.JobCriteria;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobQuery;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;

public class JobQueryRewriter {

    public PropertyQuery propertyQuery(JobQuery jobQuery, PropertyQuery propertyQuery) {
        if(jobQuery == null) {
            return propertyQuery;
        } else {
            return this.merge(this.rewrite(jobQuery), propertyQuery);
        }
    }

    private PropertyQuery merge(PropertyQuery left, PropertyQuery right) {
        if(left == null) return right;
        if(right == null) return left;

        String filter;
        if(left.opt().filter().isPresent()) {
            if(right.opt().filter().isPresent()) {
                filter = String.format("(%s) && (%s)", left.filter(), right.filter());
            } else {
                filter = left.filter();
            }
        } else {
            filter = right.filter();
        }

        String sort;
        if(left.opt().sort().isPresent()) {
            sort = left.sort();
            if(right.opt().sort().isPresent()) {
                sort = String.format("%s,%s", left.sort(), right.sort());
            }
        } else {
            sort = right.sort();
        }

        return PropertyQuery.builder()
                .filter(filter)
                .sort(sort)
                .build();
    }

    private PropertyQuery rewrite(JobQuery jobQuery) {
        if(! jobQuery.opt().criteria().isPresent() || jobQuery.criteria().isEmpty()) return null;

        StringBuilder filter = new StringBuilder();
        boolean started = false;
        for (JobCriteria criterion : jobQuery.criteria()) {
            if(criterion.opt().category().isPresent()) {
                if(started) {
                    filter.append(" && ");
                }
                started = true;

                filter.append(String.format("category == '%s'", criterion.category()));
            }

            if(criterion.opt().names().isPresent()) {
                if(started) {
                    filter.append(" && ");
                }
                started = true;

                filter.append("name IN (");
                boolean first = true;
                for (String name : criterion.names()) {
                    if(! first) {
                        filter.append(", ");
                    }
                    first = false;
                    filter.append(String.format("'%s'", name));
                }
                filter.append(")");
            }

            if(criterion.opt().runStatus().isPresent()) {
                if(started) {
                    filter.append(" && ");
                }
                started = true;

                filter.append(String.format("status.run == '%s'", criterion.runStatus()));
            }

            if(criterion.opt().exitStatus().isPresent()) {
                if(started) {
                    filter.append(" && ");
                }
                started = true;

                filter.append(String.format("status.exit == '%s'", criterion.exitStatus()));
            }
        }

        return PropertyQuery.builder().filter(filter.toString()).build();
    }
}

