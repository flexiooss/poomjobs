package org.codingmatters.poom.poomjobs.integration.lt;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.jobs.runner.service.RunnerService;
import org.codingmatters.poom.jobs.runner.service.exception.RunnerServiceInitializationException;
import org.codingmatters.poom.poomjobs.integration.lt.jobs.LTJobFactory;
import org.codingmatters.poom.poomjobs.integration.lt.jobs.LTJobProcessor;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poomjobs.client.PoomjobsJobRegistryAPIClient;
import org.codingmatters.poomjobs.client.PoomjobsJobRegistryAPIRequesterClient;
import org.codingmatters.poomjobs.client.PoomjobsRunnerRegistryAPIClient;
import org.codingmatters.poomjobs.client.PoomjobsRunnerRegistryAPIRequesterClient;
import org.codingmatters.rest.api.client.okhttp.HttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LTRunner {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(LTRunner.class);
    /**
     * mvn exec:java -Dexec.mainClass="org.codingmatters.poom.poomjobs.integration.lt.LTRunner" -Dexec.args="5"
     * @param args
     */
    public static void main(String[] args) {
        HttpClientWrapper httpClientWrapper = OkHttpClientWrapper.build();
        JsonFactory jsonFactory = new JsonFactory();

        String jobRegistryUrl = String.format("http://localhost:%s/poomjobs-jobs/v1/", 9999);
        String runnerRegistryUrl = String.format("http://localhost:%s/poomjobs-runners/v1", 9999);

        PoomjobsRunnerRegistryAPIClient runnerRegistryClient = new PoomjobsRunnerRegistryAPIRequesterClient(new OkHttpRequesterFactory(httpClientWrapper, () -> runnerRegistryUrl), jsonFactory, () -> runnerRegistryUrl);
        PoomjobsJobRegistryAPIClient jobRegistryClient = new PoomjobsJobRegistryAPIRequesterClient(new OkHttpRequesterFactory(httpClientWrapper, () -> jobRegistryUrl), jsonFactory, () -> jobRegistryUrl);

        int port = freePort();
        System.setProperty("service.url", "http://localhost:" + port);
        int concurrentJobCount = Integer.parseInt(args[0]);
        if(concurrentJobCount <= 0) throw new RuntimeException("concurentjob count should be more than 0, was " + concurrentJobCount);

        RunnerService runner = RunnerService.setup()
                .jobs(LTJobFactory.CATEGORY, new String[]{
                        LTJobFactory.JobName.QUICK.name(), LTJobFactory.JobName.SLOW.name(), LTJobFactory.JobName.VERY_SLOW.name()
                }, new LTJobFactory())
                .clients(runnerRegistryClient, jobRegistryClient)
                .concurrency(concurrentJobCount)
                .endpoint("0.0.0.0", port)
                .ttl(1000L)
                .exitOnUnrecoverableError(false)
                .build();

        try {
            runner.run();
        } catch (RunnerServiceInitializationException e) {
            e.printStackTrace();
        } finally {
            runner.stop();
        }
    }

    static int freePort() {
        System.out.println("looking up for free port");
        int port = 9998;
        do {
            System.out.println("\ttrying " + port);
            try(ServerSocket socket = new ServerSocket(port)) {
                System.out.println("\t\tok");
                return socket.getLocalPort();
            } catch (IOException e) {
                port = port - 1;
                System.out.println("\t\tnot free");
            }
        } while (port > 1024);
        throw new RuntimeException("failed finding free port");
    }
}
