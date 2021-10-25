package org.codingmatters.poom.jobs.runner.service.manager.flow;

public class JobAssignmentExcpetion extends Exception {
    public JobAssignmentExcpetion(String message) {
        super(message);
    }

    public JobAssignmentExcpetion(String message, Throwable cause) {
        super(message, cause);
    }
}
