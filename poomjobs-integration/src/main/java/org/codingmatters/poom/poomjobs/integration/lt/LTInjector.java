package org.codingmatters.poom.poomjobs.integration.lt;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.poomjobs.integration.lt.jobs.LTJobFactory;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poomjobs.client.PoomjobsJobRegistryAPIClient;
import org.codingmatters.poomjobs.client.PoomjobsJobRegistryAPIRequesterClient;
import org.codingmatters.rest.api.client.okhttp.HttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class LTInjector {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(LTInjector.class);

    /**
     *
     * Starting 100 jobs in 5 minutes with 60% quick, 30% slow and 10% (remaining) very slow
     *
     * mvn exec:java -Dexec.mainClass="org.codingmatters.poom.poomjobs.integration.lt.LTInjector" -Dexec.args="100 5 60 30"
     * @param args
     */
    public static void main(String[] args) {
        HttpClientWrapper httpClientWrapper = OkHttpClientWrapper.build();
        JsonFactory jsonFactory = new JsonFactory();

        String jobRegistryUrl = String.format("http://localhost:%s/poomjobs-jobs/v1/", 9999);
        PoomjobsJobRegistryAPIClient jobRegistryClient = new PoomjobsJobRegistryAPIRequesterClient(new OkHttpRequesterFactory(httpClientWrapper, () -> jobRegistryUrl), jsonFactory, () -> jobRegistryUrl);

        int jobCount = Integer.parseInt(args[0]);
        if(jobCount <= 0) throw new RuntimeException("job count should be stricly greather that 0, was " + jobCount);

        int injectionDuration = Integer.parseInt(args[1]);
        if(injectionDuration <= 0) throw new RuntimeException("injection duration should be stricly greather that 0, was " + injectionDuration);

        int quickPercentage = Integer.parseInt(args[2]);
        int slowPercentage = Integer.parseInt(args[3]);
        if(quickPercentage > 100) throw new RuntimeException("quick percentage should be an integer between 0 ans 100, was " + quickPercentage);
        if(slowPercentage > 100) throw new RuntimeException("quick percentage should be an integer between 0 ans 100, was " + slowPercentage);


        slowPercentage = Math.min(100 - quickPercentage, Integer.parseInt(args[3]));
        int verySlowPercentage = Math.max(0, 100 - quickPercentage - slowPercentage);

        System.out.println("#####################################################");
        System.out.printf("Starting injection of %s jobs within %s minutes.\n", jobCount, injectionDuration);
        System.out.printf("Distribution will be as follows :\n\t- %s%% of quick jobs\n\t- %s%% of slow jobs\n\t- %s%% of very slow jobs\n",
                quickPercentage, slowPercentage, verySlowPercentage
        );
        System.out.println("#####################################################");

        try {
            new LTInjector(jobRegistryClient, jobCount, injectionDuration, quickPercentage, slowPercentage)
                    .inject();
        } catch (InterruptedException e) {}
    }
    
    private final PoomjobsJobRegistryAPIClient jobRegistryClient;
    private final int jobCount;
    private final long delay;
    private List<LTJobFactory.JobName> plan = new LinkedList<>();
    private final Random rand = new Random();

    public LTInjector(
            PoomjobsJobRegistryAPIClient jobRegistryClient,
            int jobCount,
            int injectionDuration,
            int quickPercentage,
            int slowPercentage
    ) {
        this.jobRegistryClient = jobRegistryClient;
        this.jobCount = jobCount;
        
        this.delay = TimeUnit.MILLISECONDS.convert(injectionDuration, TimeUnit.MINUTES) / this.jobCount;

        for (int i = 0; i < this.jobCount * quickPercentage / 100; i++) {
            this.plan.add(LTJobFactory.JobName.QUICK);
        }
        for (int i = 0; i < this.jobCount * slowPercentage / 100; i++) {
            this.plan.add(LTJobFactory.JobName.SLOW);
        }
        while (this.plan.size() < this.jobCount) {
            this.plan.add(LTJobFactory.JobName.VERY_SLOW);
        }
    }

    private void inject() throws InterruptedException {
        int targetCount = this.plan.size();
        log.info("starting injecting {} jobs, one every {}ms.", targetCount, this.delay);
        long start = System.currentTimeMillis();

        while(! this.plan.isEmpty()) {
            try {
                String name = this.nextJob().name();
                this.jobRegistryClient.jobCollection().post(request -> request
                        .accountId("test-injector")
                        .payload(job -> {
                            job.category(LTJobFactory.CATEGORY).name(name).arguments(Collections.emptyList());
                        })
                );
                log.info("Injected a {} job", name, this.delay);
            } catch (IOException e) {
                throw new RuntimeException("failed accessing job registry", e);
            }
            Thread.sleep(this.delay);
        }
        log.info("injected {} jobs, took {}m.", targetCount, TimeUnit.MINUTES.convert(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS));
    }

    private LTJobFactory.JobName nextJob() {
        int index = this.rand.nextInt(this.plan.size());
        return this.plan.remove(index);
    }
}
