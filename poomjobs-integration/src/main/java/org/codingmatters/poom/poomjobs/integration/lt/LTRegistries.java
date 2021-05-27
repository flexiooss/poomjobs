package org.codingmatters.poom.poomjobs.integration.lt;

import org.codingmatters.poomjobs.registries.service.PoomjobRegistriesService;

import java.util.concurrent.Executors;

public class LTRegistries {
    /**
     *  mvn exec:java -Dexec.mainClass="org.codingmatters.poom.poomjobs.integration.lt.LTRegistries"
     * @param args
     */
    public static void main(String[] args) {
        PoomjobRegistriesService service = new PoomjobRegistriesService("0.0.0.0", 9999, Executors.newFixedThreadPool(5));

        service.start();
        try {
            do {
                Thread.sleep(1000);
            } while (true);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            service.stop();
        }
    }
}
