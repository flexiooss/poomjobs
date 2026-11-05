package org.codingmatters.tasks.demo.specs;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/** Exécute les règles des tâches (poom-task-service-support-specs) sur les points d'entrée en mémoire. */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/tasks")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "org.codingmatters.tasks.support.specs.steps")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, summary")
public class RunTaskSpecsTest {
}
