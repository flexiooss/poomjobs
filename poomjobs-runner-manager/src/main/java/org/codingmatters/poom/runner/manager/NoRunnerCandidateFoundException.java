package org.codingmatters.poom.runner.manager;

import java.io.IOException;

public class NoRunnerCandidateFoundException extends Exception {

    public NoRunnerCandidateFoundException(String message) {
        super(message);
    }

    public NoRunnerCandidateFoundException(String message, IOException e) {
        super(message, e);
    }
}
