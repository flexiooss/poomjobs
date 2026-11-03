package org.codingmatters.tasks.support.specs.steps;

import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.codingmatters.poomjobs.api.JobCollectionPostRequest;
import org.codingmatters.tasks.api.types.Task;
import org.codingmatters.tasks.api.types.TaskLogCreation;
import org.codingmatters.tasks.api.types.TaskNotification;
import org.codingmatters.tasks.api.types.TaskStatusChange;
import org.codingmatters.tasks.api.types.task.Status;
import org.codingmatters.tasks.support.specs.CallbackReceiver;
import org.codingmatters.tasks.support.specs.testdomain.Moments;
import org.codingmatters.tasks.support.specs.testdomain.Names;
import org.codingmatters.tasks.support.specs.testdomain.ScenarioContext;
import org.codingmatters.tasks.support.specs.testdomain.TaskVocabulary;
import org.codingmatters.tasks.support.specs.testdomain.Tasks;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class TaskSteps {
    private final ScenarioContext context;
    private final Tasks tasks;
    private TaskNotification lastNotification;

    public TaskSteps(ScenarioContext context) {
        this.context = context;
        this.tasks = new Tasks(context);
    }

    @ParameterType(TaskVocabulary.STATUS_WORDS)
    public Status statutTache(String word) {
        return TaskVocabulary.status(word);
    }

    @ParameterType(TaskVocabulary.LEVEL_WORDS)
    public TaskLogCreation.Level niveauJournal(String word) {
        return TaskVocabulary.level(word);
    }

    @ParameterType(TaskVocabulary.NOTIFICATION_WORDS)
    public TaskNotification.Type notificationTache(String word) {
        return TaskVocabulary.notification(word);
    }

    /** Issue annoncée en terminant une tâche ; vide : non précisée. */
    @ParameterType("sans préciser son issue|avec succès|en échec")
    public Optional<TaskStatusChange.Exit> issueTache(String word) {
        return switch (word) {
            case "avec succès" -> Optional.of(TaskStatusChange.Exit.SUCCESS);
            case "en échec" -> Optional.of(TaskStatusChange.Exit.FAILURE);
            default -> Optional.empty();
        };
    }

    @ParameterType("consulte|passe en cours|termine|journalise|dépose un résultat pour")
    public Tasks.Operation operationTache(String word) {
        return switch (word) {
            case "consulte" -> Tasks.Operation.RETRIEVE;
            case "passe en cours" -> Tasks.Operation.START;
            case "termine" -> Tasks.Operation.FINISH;
            case "journalise" -> Tasks.Operation.LOG;
            default -> Tasks.Operation.RESULT;
        };
    }

    @ParameterType("créée|démarrée|terminée")
    public String momentTache(String word) {
        return word;
    }

    @ParameterType(Moments.DATE_TIME_PATTERN)
    public LocalDateTime moment(String moment) {
        return Moments.parse(moment);
    }

    @ParameterType(Names.PATTERN)
    public List<String> noms(String names) {
        return Names.parse(names);
    }

    // --- Given

    /** Une tâche de ce statut existe ; elle devient la tâche du scénario. */
    @Given("une tâche {statutTache}")
    public void existingTask(Status status) {
        this.tasks.given(Tasks.CURRENT, status, false);
    }

    /** Une tâche de ce statut existe, suivie par le point de rappel du scénario. */
    @Given("une tâche {statutTache} suivie par un point de rappel")
    public void existingFollowedTask(Status status) {
        this.tasks.given(Tasks.CURRENT, status, true);
    }

    /** Une tâche de ce statut existe, désignée par ce nom dans le scénario. */
    @Given("la tâche {string} {statutTache}")
    public void existingNamedTask(String name, Status status) {
        this.tasks.given(name, status, false);
    }

    /** Une tâche de ce statut, désignée par ce nom, a été créée à ce moment. */
    @Given("la tâche {string} {statutTache} créée le {moment}")
    public void existingNamedTaskCreatedAt(String name, Status status, LocalDateTime createdAt) {
        this.tasks.given(name, status, createdAt, false);
    }

    /** Ce nombre de tâches en attente existe. */
    @Given("{int} tâches en attente")
    public void manyTasks(int count) {
        this.tasks.givenMany(TaskVocabulary.status("en attente"), count);
    }

    /** Ce journal d'information a déjà été ajouté à la tâche de ce nom. */
    @Given("le journal {string} a été ajouté à la tâche {string}")
    public void loggedTask(String text, String name) {
        this.loggedTaskAtLevel(TaskLogCreation.Level.INFO, text, name);
    }

    /** Ce journal de ce niveau a déjà été ajouté à la tâche de ce nom. */
    @Given("le journal {niveauJournal} {string} a été ajouté à la tâche {string}")
    public void loggedTaskAtLevel(TaskLogCreation.Level level, String text, String name) {
        assertThat(this.tasks.log(this.context.task(name), level, text).accepted(), is(true));
    }

    /** Un premier résultat a déjà été déposé sur la tâche du scénario. */
    @Given("un résultat a été déposé sur la tâche")
    public void resultDeposited() {
        assertThat(this.tasks.depositResult(this.context.currentTask(), Tasks.FIRST_RESULT).accepted(), is(true));
    }

    /** Le registre de jobs refuse toute soumission de job. */
    @Given("le registre de jobs refuse les jobs")
    public void jobRegistryRefuses() {
        this.tasks.jobRegistryRefuses();
    }

    /** Le registre de jobs ne répond pas (erreur d'entrée/sortie à l'appel). */
    @Given("le registre de jobs est injoignable")
    public void jobRegistryUnreachable() {
        this.tasks.jobRegistryUnreachable();
    }

    /** Le point de rappel des tâches répond par une erreur à toute notification. */
    @Given("le point de rappel répond en erreur")
    public void callbackFails() {
        this.tasks.callbackRespondsWith(500);
    }

    /** La spécification de job du point d'entrée porte ces arguments additionnels. */
    @Given("le point d'entrée ajoute les arguments {noms} aux jobs")
    public void additionalJobArguments(List<String> arguments) {
        this.context.additionalJobArguments(arguments);
    }

    /** Le point d'entrée n'a pas de dépôt de journaux. */
    @Given("le point d'entrée ne conserve pas les journaux")
    public void noLogRepository() {
        this.context.keepsLogs(false);
    }

    /** Le point d'entrée sert ses listes par pages d'au plus ce nombre d'éléments. */
    @Given("le point d'entrée liste au plus {int} tâches par page")
    public void maxPageSize(int size) {
        this.context.maxPageSize(size);
    }

    // --- When

    /** Le demandeur demande une tâche, avec la demande type, sans point de rappel. */
    @When("je demande une tâche")
    public void createTask() {
        this.tasks.create(false);
    }

    /** Le demandeur demande une tâche en donnant un point de rappel à notifier. */
    @When("je demande une tâche suivie par un point de rappel")
    public void createFollowedTask() {
        this.tasks.create(true);
    }

    /** Le demandeur consulte la tâche du scénario. */
    @When("je consulte la tâche")
    public void retrieveTask() {
        this.tasks.retrieve(this.context.currentTask());
    }

    /** L'exécutant signale la tâche du scénario en cours. */
    @When("je passe la tâche en cours")
    public void startTask() {
        this.tasks.start(this.context.currentTask());
    }

    /** L'exécutant signale la tâche du scénario terminée, avec cette issue ou sans la préciser. */
    @When("je termine la tâche {issueTache}")
    public void finishTask(Optional<TaskStatusChange.Exit> exit) {
        this.tasks.finish(this.context.currentTask(), exit.orElse(null));
    }

    /** L'exécutant ajoute ce journal de ce niveau à la tâche du scénario. */
    @When("j'ajoute le journal {niveauJournal} {string} à la tâche")
    public void logTask(TaskLogCreation.Level level, String text) {
        this.tasks.log(this.context.currentTask(), level, text);
    }

    /** Le demandeur consulte tous les journaux de la tâche de ce nom. */
    @When("je consulte les journaux de la tâche {string}")
    public void readLogs(String name) {
        this.tasks.logs(this.context.task(name), Optional.empty());
    }

    /** Le demandeur consulte les journaux de ce niveau de la tâche de ce nom. */
    @When("je consulte les journaux {niveauJournal} de la tâche {string}")
    public void readLogsAtLevel(TaskLogCreation.Level level, String name) {
        this.tasks.logs(this.context.task(name), Optional.of(level));
    }

    /** Le demandeur consulte les journaux d'une tâche qui n'existe pas. */
    @When("je consulte les journaux d'une tâche inexistante")
    public void readLogsOfUnknownTask() {
        this.tasks.logs(Tasks.UNKNOWN_ID, Optional.empty());
    }

    /** L'exécutant dépose un résultat sur la tâche du scénario. */
    @When("je dépose un résultat sur la tâche")
    public void depositResult() {
        this.tasks.depositResult(this.context.currentTask(), Tasks.FIRST_RESULT);
    }

    /** L'exécutant dépose sur la tâche du scénario un résultat différent du précédent. */
    @When("je dépose un autre résultat sur la tâche")
    public void depositOtherResult() {
        this.tasks.depositResult(this.context.currentTask(), Tasks.SECOND_RESULT);
    }

    /** Le demandeur liste les tâches, sans filtre, tri ni page. */
    @When("je liste les tâches")
    public void listTasks() {
        this.tasks.list(Optional.empty(), false, Optional.empty());
    }

    /** Le demandeur liste les tâches de ce statut (avancement seul). */
    @When("je liste les tâches {statutTache}")
    public void listTasksWithStatus(Status status) {
        this.tasks.list(Optional.of(status.run()), false, Optional.empty());
    }

    /** Le demandeur liste les tâches triées par date de création décroissante. */
    @When("je liste les tâches des plus récentes aux plus anciennes")
    public void listNewestFirst() {
        this.tasks.list(Optional.empty(), true, Optional.empty());
    }

    /** Le demandeur liste les tâches de cette position à cette position (incluses). */
    @When("je liste les tâches de la position {int} à {int}")
    public void listRange(int start, int end) {
        this.tasks.list(Optional.empty(), false, Optional.of(start + "-" + end));
    }

    /** Le demandeur liste les tâches en demandant cette page, écrite telle quelle. */
    @When("je liste les tâches de la page {string}")
    public void listPage(String page) {
        this.tasks.list(Optional.empty(), false, Optional.of(page));
    }

    /** Le demandeur ou l'exécutant applique cette action à une tâche qui n'existe pas. */
    @When("je {operationTache} une tâche inexistante")
    public void onUnknownTask(Tasks.Operation operation) {
        this.tasks.onUnknown(operation);
    }

    // --- Then

    /** La dernière action est acceptée. */
    @Then("la demande est acceptée")
    public void accepted() {
        assertThat("acceptée (statut " + this.context.lastOutcome().status() + ")", this.context.lastOutcome().accepted(), is(true));
    }

    /** La dernière action échoue sur une erreur inattendue. */
    @Then("la demande échoue sur une erreur inattendue")
    public void failedUnexpectedly() {
        assertThat("erreur inattendue (statut " + this.context.lastOutcome().status() + ")",
                this.context.lastOutcome().unexpectedError(), is(true));
    }

    /** La dernière action est refusée parce que la tâche visée n'existe pas. */
    @Then("la demande est refusée car la tâche est introuvable")
    public void refusedTaskNotFound() {
        assertThat("introuvable (statut " + this.context.lastOutcome().status() + ")", this.context.lastOutcome().notFound(), is(true));
    }

    /** La dernière action est refusée parce que la tâche visée est déjà terminée. */
    @Then("la demande est refusée car la tâche est terminée")
    public void refusedTaskDone() {
        assertThat("refusée (statut " + this.context.lastOutcome().status() + ")", this.context.lastOutcome().badRequest(), is(true));
    }

    /** La consultation des journaux est refusée : le point d'entrée ne les parcourt pas. */
    @Then("la consultation des journaux n'est pas permise")
    public void logBrowsingNotAllowed() {
        assertThat("non permise (statut " + this.context.lastOutcome().status() + ")", this.context.lastOutcome().notAllowed(), is(true));
    }

    /** La page demandée n'est pas une plage valide. */
    @Then("la page demandée est refusée")
    public void pageRefused() {
        assertThat("page refusée (statut " + this.context.lastOutcome().status() + ")",
                this.context.lastOutcome().rangeNotSatisfiable(), is(true));
    }

    /** La dernière page reçue n'est pas la fin de la liste. */
    @Then("la liste est incomplète")
    public void listIsPartial() {
        assertThat("liste incomplète (statut " + this.context.lastOutcome().status() + ")", this.context.lastOutcome().partial(), is(true));
    }

    /** La tâche du scénario est enregistrée avec ce statut. */
    @Then("la tâche est {statutTache}")
    public void taskHasStatus(Status status) {
        assertThat(this.tasks.statusOf(this.context.currentTask()), is(status));
    }

    /** La tâche du scénario porte, comme identifiant, celui sous lequel elle est enregistrée. */
    @Then("la tâche porte son identifiant")
    public void taskCarriesItsId() {
        assertThat(this.tasks.carriesItsStorageId(this.context.currentTask()), is(true));
    }

    /** La tâche du scénario a pour paramètres ceux de la demande envoyée. */
    @Then("la tâche conserve sa demande")
    public void taskKeepsRequest() {
        assertThat(this.tasks.keepsTheRequest(this.context.currentTask()), is(true));
    }

    /** La tâche du scénario a été créée, démarrée ou terminée à l'instant présent, en heure UTC. */
    @Then("la tâche a été {momentTache} à l'instant \\(heure UTC)")
    public void taskMomentIsNow(String moment) {
        Task task = this.tasks.stored(this.context.currentTask());
        LocalDateTime at = switch (moment) {
            case "créée" -> task.createdAt();
            case "démarrée" -> task.startedAt();
            default -> task.finishedAt();
        };
        assertThat(moment + " le " + at, Moments.isJustBefore(at, LocalDateTime.now(ZoneOffset.UTC)), is(true));
    }

    /** La dernière action a renvoyé la tâche du scénario, avec son statut enregistré. */
    @Then("je reçois la tâche")
    public void receivedTask() {
        this.assertIsTheCurrentTask(this.context.lastOutcome().payloadAs(Task.class));
    }

    /**
     * Un seul job a été soumis au registre pour la tâche du scénario, tel que le décrit le point d'entrée : compte
     * du job, catégorie et nom, et pour arguments l'identifiant de la tâche, l'URL des tâches et les arguments
     * additionnels.
     */
    @Then("un job est soumis pour la tâche")
    public void jobSubmitted() {
        String task = this.context.currentTask();
        List<JobCollectionPostRequest> jobs = this.tasks.jobsSubmittedFor(task);
        assertThat(jobs, hasSize(1));
        assertThat("job soumis : " + jobs.get(0), this.tasks.isTheJobOf(jobs.get(0), task), is(true));
    }

    /**
     * Le point de rappel a reçu une seule notification de ce type, signalant ce statut de la tâche (en-têtes
     * d'avancement et d'issue).
     */
    @Then("le point de rappel reçoit une notification de {notificationTache} de la tâche {statutTache}")
    public void callbackNotified(TaskNotification.Type type, Status status) {
        List<CallbackReceiver.Callback> matching = this.tasks.callbacks().stream()
                .filter(callback -> type.equals(Tasks.notificationOf(callback).type()))
                .toList();
        assertThat("notifications reçues : " + this.tasks.callbacks(), matching, hasSize(1));
        CallbackReceiver.Callback callback = matching.get(0);
        assertThat(callback.status(), is(status.run().name()));
        assertThat(callback.result(), is(status.exit() == null ? null : status.exit().name()));
        this.lastNotification = Tasks.notificationOf(callback);
    }

    /** La notification reçue porte la tâche du scénario, avec son statut enregistré. */
    @Then("la notification porte la tâche")
    public void notificationCarriesTask() {
        this.assertIsTheCurrentTask(this.lastNotification().task());
    }

    /** La notification reçue porte ce journal, rattaché à la tâche du scénario. */
    @Then("la notification porte le journal {string}")
    public void notificationCarriesLog(String text) {
        assertThat(this.lastNotification().log().log(), is(text));
        assertThat(this.lastNotification().log().taskId(), is(this.context.currentTask()));
    }

    /** Le point de rappel n'a reçu aucune notification. */
    @Then("le point de rappel ne reçoit aucune notification")
    public void noCallback() {
        assertThat(this.tasks.callbacks(), is(empty()));
    }

    /** La tâche de ce nom a exactement ces journaux conservés, dans cet ordre. */
    @Then("la tâche {string} a les journaux :")
    public void taskHasLogs(String name, List<String> logs) {
        assertThat(this.tasks.storedLogsOf(this.context.task(name)), contains(logs.toArray()));
    }

    /** La tâche de ce nom n'a aucun journal conservé. */
    @Then("la tâche {string} n'a aucun journal")
    public void taskHasNoLog(String name) {
        assertThat(this.tasks.storedLogsOf(this.context.task(name)), is(empty()));
    }

    /** Les derniers journaux lus sont exactement ceux-ci, dans cet ordre. */
    @Then("je reçois les journaux :")
    public void receivedLogs(List<String> logs) {
        assertThat(this.tasks.lastLogs(), contains(logs.toArray()));
    }

    /** La dernière lecture de journaux n'en a renvoyé aucun. */
    @Then("je ne reçois aucun journal")
    public void receivedNoLog() {
        assertThat(this.tasks.lastLogs(), is(empty()));
    }

    /** La tâche du scénario porte le premier résultat déposé. */
    @Then("la tâche porte le résultat déposé")
    public void taskCarriesResult() {
        assertThat(this.tasks.resultOf(this.context.currentTask()), is(Tasks.FIRST_RESULT));
    }

    /** La tâche du scénario porte le dernier résultat déposé, et lui seul. */
    @Then("la tâche porte le dernier résultat déposé")
    public void taskCarriesLastResult() {
        assertThat(this.tasks.resultOf(this.context.currentTask()), is(Tasks.SECOND_RESULT));
    }

    /** La dernière page reçue contient ce nombre de tâches. */
    @Then("je reçois {int} tâches")
    public void receivedTaskCount(int count) {
        assertThat(this.tasks.lastPageIds(), hasSize(count));
    }

    /** La dernière page reçue contient exactement les tâches de ces noms, dans n'importe quel ordre. */
    @Then("je reçois les tâches {noms}")
    public void receivedTasks(List<String> names) {
        assertThat(this.tasks.lastPageIds(), containsInAnyOrder(names.stream().map(this.context::task).toArray()));
    }

    /** La dernière page reçue contient exactement les tâches de ces noms, dans cet ordre. */
    @Then("je reçois, dans cet ordre, les tâches {noms}")
    public void receivedTasksInOrder(List<String> names) {
        assertThat(this.tasks.lastPageIds(), contains(names.stream().map(this.context::task).toArray()));
    }

    /**
     * Identifiant, statut et demande de la tâche du scénario ; les dates ne sont pas comparées, un dépôt pouvant
     * les tronquer (Mongo : milliseconde).
     */
    private void assertIsTheCurrentTask(Task task) {
        Task stored = this.tasks.stored(this.context.currentTask());
        assertThat(task.id(), is(stored.id()));
        assertThat(task.status(), is(stored.status()));
        assertThat(task.params(), is(stored.params()));
    }

    private TaskNotification lastNotification() {
        if (this.lastNotification == null) {
            throw new IllegalStateException("aucune notification n'a encore été vérifiée dans ce scénario");
        }
        return this.lastNotification;
    }
}
