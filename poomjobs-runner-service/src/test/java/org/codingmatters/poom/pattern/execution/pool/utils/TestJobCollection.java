package org.codingmatters.poom.pattern.execution.pool.utils;

import org.codingmatters.poom.handler.HandlerResource;
import org.codingmatters.poomjobs.api.JobCollectionGetRequest;
import org.codingmatters.poomjobs.api.JobCollectionGetResponse;
import org.codingmatters.poomjobs.api.jobcollectiongetresponse.Status200;
import org.codingmatters.poomjobs.api.types.Job;

import java.util.ArrayList;
import java.util.List;

public class TestJobCollection extends HandlerResource<JobCollectionGetRequest, JobCollectionGetResponse> {

    private final List<Job> currentJobs = new ArrayList<>();

    @Override
    protected JobCollectionGetResponse defaultResponse(JobCollectionGetRequest jobCollectionGetRequest) {
        List<Job> currentJobs1 = new ArrayList<>(currentJobs);
        currentJobs.clear();
        return JobCollectionGetResponse.builder()
                .status200(Status200.builder()
                        .payload(currentJobs1)
                        .build())
                .build();
    }

    public List<Job> currentJobs() {
        return currentJobs;
    }
}

