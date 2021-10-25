package org.codingmatters.poom.client;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poomjobs.api.JobCollectionPostRequest;
import org.codingmatters.poomjobs.api.types.JobCreationData;
import org.codingmatters.poomjobs.client.PoomjobsJobRegistryAPIRequesterClient;
import org.codingmatters.rest.api.client.okhttp.HttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;

public class PoomjobsJobRegistryMicrobenchmark {

    public static final String URL = "http://localhost:8888/nothing/there";

    /**
     * MAVEN_OPTS=-Xmx64m mvn exec:java -Dexec.mainClass=org.codingmatters.poom.client.PoomjobsJobRegistryMicrobenchmark -Dexec.classpathScope="test"
     * @param args
     */
    public static void main(String[] args) {
        PoomjobsJobRegistryMicrobenchmark bench = new PoomjobsJobRegistryMicrobenchmark();
        try {
            bench.setup();
        } catch(Exception e) {
            throw new RuntimeException("error in benchmark setup", e);
        }
        try {
            bench.run();
        } catch(Exception e) {
            throw new RuntimeException("error in benchmark exec", e);
        }
    }


    private PoomjobsJobRegistryAPIRequesterClient apiClient;
    private final JsonFactory jsonFactory = new JsonFactory();
    private final HttpClientWrapper client = OkHttpClientWrapper.build();

    private void setup() throws Exception {
        System.out.println("will use url : " + URL);
        System.out.println("press enter to start");
        System.in.read();

        this.apiClient = new PoomjobsJobRegistryAPIRequesterClient(
                new OkHttpRequesterFactory(client, () -> URL),
                jsonFactory,
                URL
        );
    }

    private void run() throws Exception {
        long count = 0;
        while(true) {
            this.unit();
            count++;
            if(count % 10000 == 0) {
                System.out.printf("%010d\n", count);
            }
        }
    }

    private void unit() throws Exception {
        try {
            this.apiClient.jobCollection().post(JobCollectionPostRequest.builder()
                    .payload(JobCreationData.builder()
                            .arguments("arg", "ument")
                            .category("categ")
                            .name("yob")
                            .build())
                    .build());
        } catch(Exception e) {}
    }
}
