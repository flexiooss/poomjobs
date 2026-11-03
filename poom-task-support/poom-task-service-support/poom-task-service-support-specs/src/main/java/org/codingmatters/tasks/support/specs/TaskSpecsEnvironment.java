package org.codingmatters.tasks.support.specs;

import java.util.List;
import java.util.ServiceLoader;

/**
 * Environnement des scénarios, construit une fois par JVM : l'adaptateur éprouvé (trouvé par {@link ServiceLoader}),
 * le registre de jobs simulé et le point de rappel local.
 */
public final class TaskSpecsEnvironment {
    private static TaskSpecsEnvironment instance;

    private final TaskEntryPointsUnderTest underTest;
    private final JobRegistryDouble jobRegistry = new JobRegistryDouble();
    private final CallbackReceiver callbacks = new CallbackReceiver();

    private TaskSpecsEnvironment(TaskEntryPointsUnderTest underTest) {
        this.underTest = underTest;
    }

    public static synchronized TaskSpecsEnvironment get() {
        if (instance == null) {
            instance = new TaskSpecsEnvironment(loadUnderTest());
        }
        return instance;
    }

    public TaskEntryPointsUnderTest underTest() {
        return this.underTest;
    }

    public JobRegistryDouble jobRegistry() {
        return this.jobRegistry;
    }

    public CallbackReceiver callbacks() {
        return this.callbacks;
    }

    /** Remet tâches, journaux, jobs soumis et notifications à zéro ; registre et rappel acceptent de nouveau. */
    public void reset() {
        this.underTest.reset();
        this.jobRegistry.reset();
        this.callbacks.reset();
    }

    private static TaskEntryPointsUnderTest loadUnderTest() {
        List<TaskEntryPointsUnderTest> found = ServiceLoader.load(TaskEntryPointsUnderTest.class).stream()
                .map(ServiceLoader.Provider::get)
                .toList();
        if (found.size() != 1) {
            throw new IllegalStateException("une et une seule implémentation de " + TaskEntryPointsUnderTest.class.getName()
                    + " attendue dans META-INF/services, trouvé : " + found);
        }
        return found.get(0);
    }
}
