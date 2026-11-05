package org.codingmatters.tasks.support.specs.testdomain;

import org.codingmatters.tasks.support.specs.TaskSpecsEnvironment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * État partagé par les steps d'un scénario (une instance par scénario, injectée par picocontainer) : compte
 * courant, réglages du point d'entrée, tâches désignées par un nom propre au scénario (la tâche courante est la
 * dernière créée ou donnée), dernière réponse reçue.
 */
public class ScenarioContext {
    public static final String DEFAULT_ACCOUNT = "acme";

    /** Taille de page maximale des listes quand le scénario n'en fixe pas. */
    public static final int DEFAULT_MAX_PAGE_SIZE = 100;

    private final TaskSpecsEnvironment environment = TaskSpecsEnvironment.get();
    private final Map<String, String> taskIdsByName = new HashMap<>();
    private String currentTaskId;
    private Outcome lastOutcome;
    private int maxPageSize = DEFAULT_MAX_PAGE_SIZE;
    private boolean keepsLogs = true;
    private List<String> additionalJobArguments = List.of();

    public TaskSpecsEnvironment environment() {
        return this.environment;
    }

    public String account() {
        return DEFAULT_ACCOUNT;
    }

    public void rememberTask(String name, String id) {
        this.taskIdsByName.put(name, id);
        this.currentTaskId = id;
    }

    public String task(String name) {
        String id = this.taskIdsByName.get(name);
        if (id == null) {
            throw new IllegalStateException("aucune tâche \"" + name + "\" connue dans ce scénario");
        }
        return id;
    }

    /** Dernière tâche créée ou donnée dans le scénario. */
    public String currentTask() {
        if (this.currentTaskId == null) {
            throw new IllegalStateException("aucune tâche n'a encore été créée dans ce scénario");
        }
        return this.currentTaskId;
    }

    public Outcome lastOutcome() {
        if (this.lastOutcome == null) {
            throw new IllegalStateException("aucune action n'a encore été faite dans ce scénario");
        }
        return this.lastOutcome;
    }

    public void lastOutcome(Outcome outcome) {
        this.lastOutcome = outcome;
    }

    public int maxPageSize() {
        return this.maxPageSize;
    }

    public void maxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }

    public boolean keepsLogs() {
        return this.keepsLogs;
    }

    public void keepsLogs(boolean keepsLogs) {
        this.keepsLogs = keepsLogs;
    }

    public List<String> additionalJobArguments() {
        return this.additionalJobArguments;
    }

    public void additionalJobArguments(List<String> arguments) {
        this.additionalJobArguments = List.copyOf(arguments);
    }
}
