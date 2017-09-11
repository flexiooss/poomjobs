package org.codingmatters.poom.client.explore;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.codingmatters.poomjobs.api.JobCollectionGetRequest;
import org.codingmatters.poomjobs.api.JobCollectionGetResponse;
import org.codingmatters.poomjobs.api.jobcollectiongetresponse.Status200;
import org.codingmatters.poomjobs.api.jobcollectiongetresponse.Status206;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.json.JobReader;
import org.codingmatters.rest.api.client.Requester;
import org.codingmatters.rest.api.client.RequesterFactory;
import org.codingmatters.rest.api.client.ResponseDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class PoomjobsAPIRequesterClient implements PoomjobsAPIClient {

    static private final Logger log = LoggerFactory.getLogger(PoomjobsAPIRequesterClient.class);

    private final RequesterFactory requesterFactory;
    private final JsonFactory jsonFactory;
    private final String baseUrl;

    public PoomjobsAPIRequesterClient(RequesterFactory requesterFactory, JsonFactory jsonFactory, String baseUrl) {
        this.requesterFactory = requesterFactory;
        this.jsonFactory = jsonFactory;
        this.baseUrl = baseUrl;
    }

    @Override
    public JobCollectionClient jobCollection() {
        return new JobCollectionClient() {
            @Override
            public JobCollectionGetResponse get(JobCollectionGetRequest req) throws IOException {
                Requester requester = requesterFactory
                        .forBaseUrl(baseUrl)
                        .path("/jobs");
                if(req.range() != null) {
                    requester.parameter("range", req.range());
                }

                ResponseDelegate response = requester.get();
                if(response.code() == 200) {
                    return JobCollectionGetResponse.builder()
                            .status200(Status200.builder()
                                    .payload(readListValue(jsonFactory.createParser(response.body())))
                                    .acceptRange(response.header("accept-range"))
                                    .contentRange(response.header("content-range"))
                                    .build())
                            .build();
                }
                if(response.code() == 206) {
                    return JobCollectionGetResponse.builder()
                            .status206(Status206.builder()
                                    .payload(readListValue(jsonFactory.createParser(response.body())))
                                    .acceptRange(response.header("accept-range"))
                                    .contentRange(response.header("content-range"))
                                    .build())
                            .build();
                }
                return null;
            }
        };
    }

    static private List<Job> readListValue(JsonParser parser) throws IOException {
        parser.nextToken();
        if (parser.currentToken() == JsonToken.VALUE_NULL) return null;
        if (parser.currentToken() == JsonToken.START_ARRAY) {
            LinkedList<Job> listValue = new LinkedList<>();
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                if(parser.currentToken() == JsonToken.VALUE_NULL) {
                    listValue.add(null);
                } else {
                    listValue.add(new JobReader().read(parser));
                }
            }
            return listValue;
        }
        throw new IOException("failed reading Job list");
    }
}
