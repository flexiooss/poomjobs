package org.codingmatters.poom.client.explore;

import org.codingmatters.poomjobs.api.JobCollectionGetRequest;
import org.codingmatters.poomjobs.api.JobCollectionGetResponse;

import java.io.IOException;

public interface PoomjobsAPIClient {
    JobCollectionClient jobCollection();

    interface JobCollectionClient {
        JobCollectionGetResponse get(JobCollectionGetRequest build) throws IOException;
    }
}
