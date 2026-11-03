package org.codingmatters.tasks.support.specs.steps;

import io.cucumber.java.Before;
import org.codingmatters.tasks.support.specs.testdomain.ScenarioContext;

public class Hooks {
    private final ScenarioContext context;

    public Hooks(ScenarioContext context) {
        this.context = context;
    }

    @Before
    public void resetData() {
        this.context.environment().reset();
    }
}
