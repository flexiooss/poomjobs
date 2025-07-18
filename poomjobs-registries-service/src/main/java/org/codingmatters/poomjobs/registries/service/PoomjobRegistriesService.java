package org.codingmatters.poomjobs.registries.service;

import com.fasterxml.jackson.core.JsonFactory;
import io.undertow.Undertow;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poomjobs.client.PoomjobsRunnerRegistryAPIHandlersClient;
import org.codingmatters.poom.poomjobs.domain.jobs.repositories.JobRepository;
import org.codingmatters.poom.poomjobs.domain.runners.repositories.RunnerRepository;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerQuery;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.runner.manager.DefaultRunnerClientFactory;
import org.codingmatters.poom.runner.manager.RunnerInvokerListener;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.poomjobs.service.PoomjobsJobRegistryAPI;
import org.codingmatters.poomjobs.service.PoomjobsRunnerRegistryAPI;
import org.codingmatters.poomjobs.service.api.PoomjobsJobRegistryAPIProcessor;
import org.codingmatters.poomjobs.service.api.PoomjobsRunnerRegistryAPIProcessor;
import org.codingmatters.rest.api.Processor;
import org.codingmatters.rest.api.client.okhttp.OkHttpClientWrapper;
import org.codingmatters.rest.api.processors.MatchingPathProcessor;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PoomjobRegistriesService {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(PoomjobRegistriesService.class);

    static public final String CLIENT_POOL_SIZE = "CLIENT_POOL_SIZE";

    public static void main(String[] args) {
        String host = Env.mandatory(Env.SERVICE_HOST).asString();
        int port = Integer.parseInt(Env.mandatory(Env.SERVICE_PORT).asString());
        int clientPoolSize = Env.optional(CLIENT_POOL_SIZE).orElse(new Env.Var("5")).asInteger();
        
        AtomicInteger threadIndex = new AtomicInteger(1);
        ExecutorService clientPool = Executors.newFixedThreadPool(clientPoolSize, runnable -> new Thread(runnable, "client-pool-thread-" + threadIndex.getAndIncrement()));
        ExecutorService listenerPool = Executors.newFixedThreadPool(Env.optional("JOB_LISTENER_POOL_SIZE").orElse(new Env.Var("5")).asInteger());

        PoomjobRegistriesService service = new PoomjobRegistriesService(host, port, clientPool, listenerPool);
        service.start();

        log.info("poomjob registries running.");
        while(true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
        log.info("poomjob registries stopping...");
        service.stop();
        log.info("poomjob registries stopped.");
    }


    private final String host;
    private final int port;
    private final ExecutorService clientPool;

    private Undertow server;
    private final JsonFactory jsonFactory = new JsonFactory();

    private final Repository<JobValue, PropertyQuery> jobRepository = JobRepository.createInMemory();
    private final PoomjobsJobRegistryAPI jobRegistryAPI;

    private final PoomjobsRunnerRegistryAPIHandlersClient runnerRegistryClient;

    private final Repository<RunnerValue, RunnerQuery> runnerRepository = RunnerRepository.createInMemory();
    private final PoomjobsRunnerRegistryAPI runnerRegistryAPI;

    public PoomjobRegistriesService(String host, int port, ExecutorService clientPool, ExecutorService listenerPool) {
        this.host = host;
        this.port = port;
        this.clientPool = clientPool;

        this.runnerRegistryAPI = new PoomjobsRunnerRegistryAPI(this.runnerRepository, new JsonFactory());
        this.runnerRegistryClient = new PoomjobsRunnerRegistryAPIHandlersClient(
                this.runnerRegistryAPI.handlers(),
                this.clientPool
        );

        RunnerInvokerListener runnerInvokerListener = new RunnerInvokerListener(runnerRegistryClient, new DefaultRunnerClientFactory(this.jsonFactory, OkHttpClientWrapper.build()), listenerPool);
        this.jobRegistryAPI = new PoomjobsJobRegistryAPI(
                this.jobRepository,
                runnerInvokerListener,
                null,
                new JsonFactory()
        );
    }

    public void start() {
        Processor processor = MatchingPathProcessor
                .whenMatching("/poomjobs-jobs/v1/.*", new PoomjobsJobRegistryAPIProcessor(
                        "/poomjobs-jobs/v1",
                        this.jsonFactory,
                        this.jobRegistryAPI.handlers()
                ))
                .whenMatching("/poomjobs-runners/v1/.*", new PoomjobsRunnerRegistryAPIProcessor(
                        "/poomjobs-runners/v1",
                        this.jsonFactory,
                        this.runnerRegistryAPI.handlers()
                )).whenNoMatch((requestDelegate, responseDelegate) ->
                        responseDelegate.status(404).contenType("text/plain").payload("nothing here..." + requestDelegate.path(), "UTF-8")
                );

        this.server = Undertow.builder()
                .addHttpListener(this.port, this.host)
                .setHandler(new CdmHttpUndertowHandler(processor))
                .build();
        this.server.start();
    }

    public void stop() {
        this.server.stop();

        try {
            this.clientPool.shutdownNow();
        } catch (Exception e) {}

        this.clientPool.shutdown();
        try {
            this.clientPool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {}
        if(! this.clientPool.isTerminated()) {
            this.clientPool.shutdownNow();
            try {
                this.clientPool.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException("cannot stop runner registry pool properly", e);
            }
        }
    }
}
