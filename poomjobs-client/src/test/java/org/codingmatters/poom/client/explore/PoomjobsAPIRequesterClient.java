package org.codingmatters.poom.client.explore;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.client.explore.jobcollection.JobCollectionRequesterClient;
import org.codingmatters.rest.api.client.RequesterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PoomjobsAPIRequesterClient implements PoomjobsAPIClient {

    static private final Logger log = LoggerFactory.getLogger(PoomjobsAPIRequesterClient.class);

    private final RequesterFactory requesterFactory;
    private final JsonFactory jsonFactory;
    private final String baseUrl;

    private final JobCollectionClient jobCollectionDeleguate;

    public PoomjobsAPIRequesterClient(RequesterFactory requesterFactory, JsonFactory jsonFactory, String baseUrl) {
        this.requesterFactory = requesterFactory;
        this.jsonFactory = jsonFactory;
        this.baseUrl = baseUrl;

        this.jobCollectionDeleguate = new JobCollectionRequesterClient(this.requesterFactory, this.jsonFactory, this.baseUrl);
    }

    @Override
    public JobCollectionClient jobCollection() {
        return this.jobCollectionDeleguate;
    }

}
