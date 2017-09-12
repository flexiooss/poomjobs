package org.codingmatters.poom.client.explore;

import org.codingmatters.poomjobs.api.JobCollectionGetRequest;
import org.codingmatters.poomjobs.api.JobCollectionGetResponse;
import org.codingmatters.poomjobs.api.JobCollectionPostRequest;
import org.codingmatters.poomjobs.api.JobCollectionPostResponse;

import java.io.IOException;
import java.util.function.Consumer;

public interface PoomjobsAPIClient {
    JobCollectionClient jobCollection();

    interface JobCollectionClient {
        JobCollectionGetResponse get(JobCollectionGetRequest build) throws IOException;

        JobCollectionPostResponse post(JobCollectionPostRequest build) throws IOException;
        JobCollectionPostResponse post(Consumer<JobCollectionPostRequest.Builder> builder) throws IOException;
    }
}
