package org.codingmatters.tasks.support.specs.testdomain;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.poom.services.domain.entities.PagedEntityList;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poomjobs.api.JobCollectionPostRequest;
import org.codingmatters.rest.api.client.Requester;
import org.codingmatters.tasks.api.TaskCollectionGetRequest;
import org.codingmatters.tasks.api.TaskCollectionPostRequest;
import org.codingmatters.tasks.api.TaskEntityGetRequest;
import org.codingmatters.tasks.api.TaskLogsGetRequest;
import org.codingmatters.tasks.api.TaskLogsPostRequest;
import org.codingmatters.tasks.api.TaskResultsPutRequest;
import org.codingmatters.tasks.api.TaskStatusChangesPostRequest;
import org.codingmatters.tasks.api.types.Task;
import org.codingmatters.tasks.api.types.TaskLog;
import org.codingmatters.tasks.api.types.TaskLogCreation;
import org.codingmatters.tasks.api.types.TaskNotification;
import org.codingmatters.tasks.api.types.TaskStatusChange;
import org.codingmatters.tasks.api.types.json.TaskNotificationReader;
import org.codingmatters.tasks.api.types.task.Status;
import org.codingmatters.tasks.support.api.TaskEntryPointAdapter;
import org.codingmatters.tasks.support.handlers.tasks.BrowseTaskLogs;
import org.codingmatters.tasks.support.handlers.tasks.BrowseTasks;
import org.codingmatters.tasks.support.handlers.tasks.CreateTask;
import org.codingmatters.tasks.support.handlers.tasks.CreateTaskLog;
import org.codingmatters.tasks.support.handlers.tasks.ReplaceTaskResult;
import org.codingmatters.tasks.support.handlers.tasks.RetrieveTask;
import org.codingmatters.tasks.support.handlers.tasks.UpdateTaskStatus;
import org.codingmatters.tasks.support.specs.CallbackReceiver;
import org.codingmatters.value.objects.values.ObjectValue;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Couche domaine de test des tâches : fixtures (insertion par les dépôts de l'adaptateur éprouvé), actions (appel
 * direct des handlers génériques construits autour de l'adaptateur, avec les réglages du scénario), lecture de
 * l'état, des jobs soumis au registre et des notifications reçues par le point de rappel. Seule à connaître les
 * dépôts, les requêtes générées et les codes de réponse.
 */
public class Tasks {
    private static final int ALL = 10_000;
    private static final JsonFactory JSON = new JsonFactory();

    /** Identifiant qu'aucune tâche ne porte : cible des actions sur une tâche inexistante. */
    public static final String UNKNOWN_ID = "tache-inexistante";

    /** Nom de la tâche courante quand le scénario n'en nomme qu'une. */
    public static final String CURRENT = "";

    private static final LocalDateTime FIXTURE_CREATION = LocalDateTime.of(2026, 1, 5, 9, 30);

    /** Demande type d'une tâche : ses paramètres. */
    public static final ObjectValue SAMPLE_PARAMS = ObjectValue.builder()
            .property("depart", v -> v.stringValue("Paris"))
            .property("arrivee", v -> v.stringValue("Lyon"))
            .build();

    /** Résultats types : le premier déposé, puis celui qui le remplace. */
    public static final ObjectValue FIRST_RESULT = ObjectValue.builder().property("chemin", v -> v.stringValue("Paris-Dijon-Lyon")).build();
    public static final ObjectValue SECOND_RESULT = ObjectValue.builder().property("chemin", v -> v.stringValue("Paris-Lyon")).build();

    /** Actions sur une tâche, nommées dans le Gherkin (cf. {@link #onUnknown}). */
    public enum Operation { RETRIEVE, START, FINISH, LOG, RESULT }

    private final ScenarioContext context;
    private List<String> lastLogs;

    public Tasks(ScenarioContext context) {
        this.context = context;
    }

    // --- fixtures

    /** Insère une tâche de ce statut, créée le 5 janvier 2026, suivie ou non par le point de rappel du scénario. */
    public String given(String name, Status status, boolean followed) {
        return this.given(name, status, FIXTURE_CREATION, followed);
    }

    /** Insère une tâche de ce statut créée à ce moment. */
    public String given(String name, Status status, LocalDateTime createdAt, boolean followed) {
        return this.insert(name, Task.builder()
                .status(status)
                .createdAt(createdAt)
                .params(SAMPLE_PARAMS)
                .callbackUrl(followed ? this.callbackUrl() : null)
                .build());
    }

    /** Insère une tâche en cours, démarrée à ce moment. */
    public String givenStarted(String name, LocalDateTime startedAt) {
        return this.insert(name, Task.builder()
                .status(Status.builder().run(Status.Run.RUNNING).build())
                .createdAt(FIXTURE_CREATION)
                .startedAt(startedAt)
                .params(SAMPLE_PARAMS)
                .build());
    }

    /** Pose ce résultat sur la tâche, directement dans le dépôt (quel que soit son statut). */
    public void givenResult(String task, ObjectValue result) {
        try {
            Repository<Task, PropertyQuery> repository = this.base().tasks();
            Entity<Task> entity = repository.retrieve(task);
            repository.update(entity, entity.value().withResults(result));
        } catch (RepositoryException e) {
            throw new IllegalStateException("dépôt du résultat impossible", e);
        }
    }

    private String insert(String name, Task task) {
        try {
            Repository<Task, PropertyQuery> repository = this.base().tasks();
            Entity<Task> created = repository.create(task);
            repository.update(created, created.value().withId(created.id()));
            this.context.rememberTask(name, created.id());
            return created.id();
        } catch (RepositoryException e) {
            throw new IllegalStateException("insertion de la tâche impossible", e);
        }
    }

    public void givenMany(Status status, int count) {
        for (int i = 0; i < count; i++) {
            this.given("n°" + i, status, false);
        }
    }

    // --- actions

    /** Demande une tâche avec la demande type, suivie ou non par le point de rappel. */
    public Outcome create(boolean followed) {
        TaskCollectionPostRequest request = TaskCollectionPostRequest.builder()
                .callbackUrl(followed ? this.callbackUrl() : null)
                .payload(SAMPLE_PARAMS)
                .build();
        Outcome outcome = Outcome.of(new CreateTask(this.adapter(), this.context.environment().jobRegistry().client(), JSON)
                .apply(request));
        if (outcome.accepted()) {
            this.context.rememberTask(CURRENT, outcome.payloadAs(Task.class).id());
        } else {
            this.lastCreatedInStore().ifPresent(id -> this.context.rememberTask(CURRENT, id));
        }
        return this.done(outcome);
    }

    public Outcome retrieve(String task) {
        return this.done(Outcome.of(new RetrieveTask(this.adapter(), JSON)
                .apply(TaskEntityGetRequest.builder().taskId(task).build())));
    }

    /** Signale la tâche en cours. */
    public Outcome start(String task) {
        return this.changeStatus(task, TaskStatusChange.builder().run(TaskStatusChange.Run.RUNNING).build());
    }

    /** Signale la tâche terminée, avec cette issue ({@code null} : issue non précisée). */
    public Outcome finish(String task, TaskStatusChange.Exit exit) {
        return this.changeStatus(task, TaskStatusChange.builder().run(TaskStatusChange.Run.DONE).exit(exit).build());
    }

    /** Ajoute une ligne de journal de ce niveau à la tâche. */
    public Outcome log(String task, TaskLogCreation.Level level, String text) {
        return this.done(Outcome.of(new CreateTaskLog(this.adapter(), JSON).apply(TaskLogsPostRequest.builder()
                .taskId(task)
                .payload(TaskLogCreation.builder().level(level).log(text).build())
                .build())));
    }

    /** Lit les journaux de la tâche, restreints ou non à un niveau ; ils sont retenus si la liste est servie. */
    public Outcome logs(String task, Optional<TaskLogCreation.Level> level) {
        Outcome outcome = this.done(Outcome.of(new BrowseTaskLogs(this.adapter(), this.context.maxPageSize(), JSON)
                .apply(TaskLogsGetRequest.builder()
                        .taskId(task)
                        .filter(level.map(l -> "level == '" + l.name() + "'").orElse(null))
                        .build())));
        if (outcome.accepted()) {
            this.lastLogs = new ArrayList<>();
            for (Object log : items(outcome)) {
                this.lastLogs.add(((TaskLog) log).log());
            }
        }
        return outcome;
    }

    /** Liste les tâches, restreintes ou non à un statut, des plus récentes aux plus anciennes ou non, sur cette page ou non. */
    public Outcome list(Optional<Status.Run> run, boolean newestFirst, Optional<String> range) {
        return this.done(Outcome.of(new BrowseTasks(this.adapter(), this.context.maxPageSize(), JSON)
                .apply(TaskCollectionGetRequest.builder()
                        .filter(run.map(r -> "status.run == '" + r.name() + "'").orElse(null))
                        .orderBy(newestFirst ? "createdAt desc" : null)
                        .range(range.orElse(null))
                        .build())));
    }

    /** Dépose ce résultat sur la tâche. */
    public Outcome depositResult(String task, ObjectValue result) {
        return this.done(Outcome.of(new ReplaceTaskResult(this.adapter(), JSON)
                .apply(TaskResultsPutRequest.builder().taskId(task).payload(result).build())));
    }

    /** Applique cette action à une tâche qui n'existe pas. */
    public Outcome onUnknown(Operation operation) {
        return switch (operation) {
            case RETRIEVE -> this.retrieve(UNKNOWN_ID);
            case START -> this.start(UNKNOWN_ID);
            case FINISH -> this.finish(UNKNOWN_ID, null);
            case LOG -> this.log(UNKNOWN_ID, TaskLogCreation.Level.INFO, "journal");
            case RESULT -> this.depositResult(UNKNOWN_ID, FIRST_RESULT);
        };
    }

    // --- état

    public Task stored(String task) {
        try {
            return Optional.ofNullable(this.base().tasks().retrieve(task)).map(Entity::value)
                    .orElseThrow(() -> new AssertionError("tâche " + task + " absente"));
        } catch (RepositoryException e) {
            throw new IllegalStateException("lecture de la tâche impossible", e);
        }
    }

    public Status statusOf(String task) {
        return this.stored(task).status();
    }

    /** La tâche porte comme identifiant celui sous lequel le dépôt l'a enregistrée. */
    public boolean carriesItsStorageId(String task) {
        return task.equals(this.stored(task).id());
    }

    public boolean keepsTheRequest(String task) {
        return SAMPLE_PARAMS.equals(this.stored(task).params());
    }

    public ObjectValue resultOf(String task) {
        return this.stored(task).results();
    }

    /** Journaux conservés pour la tâche par le dépôt de l'adaptateur, dans l'ordre du dépôt. */
    public List<String> storedLogsOf(String task) {
        try {
            Repository<TaskLog, PropertyQuery> logs = this.base().taskLogs()
                    .orElseThrow(() -> new IllegalStateException("l'adaptateur éprouvé doit offrir les journaux"));
            PagedEntityList<TaskLog> found = logs.search(PropertyQuery.builder().filter("taskId == '%s'", task).build(), 0, ALL - 1);
            return found.stream().map(entity -> entity.value().log()).toList();
        } catch (RepositoryException e) {
            throw new IllegalStateException("lecture des journaux impossible", e);
        }
    }

    public List<String> lastLogs() {
        if (this.lastLogs == null) {
            throw new IllegalStateException("aucun journal n'a encore été lu dans ce scénario");
        }
        return this.lastLogs;
    }

    /** Identifiants des tâches de la dernière page reçue, dans l'ordre de la page. */
    public List<String> lastPageIds() {
        List<String> ids = new ArrayList<>();
        for (Object task : items(this.context.lastOutcome())) {
            ids.add(((Task) task).id());
        }
        return ids;
    }

    /** Jobs soumis au registre pour cette tâche. */
    public List<JobCollectionPostRequest> jobsSubmittedFor(String task) {
        return this.context.environment().jobRegistry().submitted().stream()
                .filter(job -> job.payload().arguments() != null && task.equals(job.payload().arguments().get(0)))
                .toList();
    }

    public int jobsSubmittedCount() {
        return this.context.environment().jobRegistry().submitted().size();
    }

    /**
     * Le job est celui que décrit le point d'entrée pour cette tâche : compte du job, catégorie et nom de sa
     * spécification, arguments (identifiant de la tâche, URL des tâches, arguments additionnels).
     */
    public boolean isTheJobOf(JobCollectionPostRequest job, String task) {
        TaskEntryPointAdapter adapter = this.adapter().get();
        TaskEntryPointAdapter.JobSpec spec = adapter.jobSpecFor(this.stored(task));
        List<String> arguments = new ArrayList<>(List.of(task, spec.tasksUrl));
        arguments.addAll(List.of(spec.additionalArgs));
        return Objects.equals(adapter.jobAccount(), job.accountId())
                && Objects.equals(spec.category, job.payload().category())
                && Objects.equals(spec.name, job.payload().name())
                && arguments.equals(job.payload().arguments().stream().toList());
    }

    public List<CallbackReceiver.Callback> callbacks() {
        return this.context.environment().callbacks().received();
    }

    /** Notification portée par le corps d'un rappel. */
    public static TaskNotification notificationOf(CallbackReceiver.Callback callback) {
        try (JsonParser parser = JSON.createParser(callback.body())) {
            return Objects.requireNonNull(new TaskNotificationReader().read(parser));
        } catch (IOException e) {
            throw new IllegalStateException("notification illisible : " + callback.body(), e);
        }
    }

    // --- dépendances et réglages du point d'entrée

    public void jobRegistryRefuses() {
        this.context.environment().jobRegistry().refuseJobs();
    }

    public void jobRegistryUnreachable() {
        this.context.environment().jobRegistry().becomeUnreachable();
    }

    public void callbackRespondsWith(int code) {
        this.context.environment().callbacks().respondWith(code);
    }

    // --- interne

    private Outcome changeStatus(String task, TaskStatusChange change) {
        return this.done(Outcome.of(new UpdateTaskStatus(this.adapter(), JSON)
                .apply(TaskStatusChangesPostRequest.builder().taskId(task).payload(change).build())));
    }

    private Optional<String> lastCreatedInStore() {
        try {
            List<Entity<Task>> all = this.base().tasks().all(0, ALL - 1);
            return all.isEmpty() ? Optional.empty() : Optional.of(all.get(all.size() - 1).id());
        } catch (RepositoryException e) {
            throw new IllegalStateException("lecture des tâches impossible", e);
        }
    }

    /** Adaptateur éprouvé, tel que fourni pour le compte du scénario. */
    private TaskEntryPointAdapter base() {
        return this.context.environment().underTest().adapter(this.context.account());
    }

    /** Adaptateur donné aux handlers : l'adaptateur éprouvé, avec ou sans journaux, avec les arguments additionnels du scénario. */
    private Supplier<TaskEntryPointAdapter> adapter() {
        TaskEntryPointAdapter base = this.base();
        boolean keepsLogs = this.context.keepsLogs();
        String[] additional = this.context.additionalJobArguments().toArray(String[]::new);
        TaskEntryPointAdapter adapted = new TaskEntryPointAdapter() {
            @Override
            public Repository<Task, PropertyQuery> tasks() {
                return base.tasks();
            }

            @Override
            public Optional<Repository<TaskLog, PropertyQuery>> taskLogs() {
                return keepsLogs ? base.taskLogs() : Optional.empty();
            }

            @Override
            public JobSpec jobSpecFor(Task task) {
                JobSpec spec = base.jobSpecFor(task);
                return additional.length == 0 ? spec : new JobSpec(spec.category, spec.name, spec.tasksUrl, additional);
            }

            @Override
            public String jobAccount() {
                return base.jobAccount();
            }

            @Override
            public Requester callbackRequester(String callbackUrl) {
                return base.callbackRequester(callbackUrl);
            }
        };
        return () -> adapted;
    }

    private String callbackUrl() {
        return this.context.environment().callbacks().url(this.context.account());
    }

    private static Iterable<?> items(Outcome outcome) {
        return outcome.payload().map(payload -> (Iterable<?>) payload).orElse(List.of());
    }

    private Outcome done(Outcome outcome) {
        this.context.lastOutcome(outcome);
        return outcome;
    }
}
