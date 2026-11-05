package org.codingmatters.tasks.support.specs.testdomain;

import org.codingmatters.tasks.api.types.TaskLogCreation;
import org.codingmatters.tasks.api.types.TaskNotification;
import org.codingmatters.tasks.api.types.task.Status;

/**
 * Mots du Gherkin des tâches et leur valeur technique :
 * <ul>
 *     <li>statut : en attente = PENDING, en cours = RUNNING, terminée avec succès = DONE/SUCCESS,
 *     terminée en échec = DONE/FAILURE ;</li>
 *     <li>niveau de journal : d'information = INFO, d'avertissement = WARN, d'erreur = ERROR ;</li>
 *     <li>notification : création = CREATED, changement de statut = STATUS_CHANGE, nouveau journal = LOG_APPENDED.</li>
 * </ul>
 */
public final class TaskVocabulary {
    public static final String STATUS_WORDS = "en attente|en cours|terminée avec succès|terminée en échec";
    public static final String LEVEL_WORDS = "d'information|d'avertissement|d'erreur";
    public static final String NOTIFICATION_WORDS = "création|changement de statut|nouveau journal";

    private TaskVocabulary() {
    }

    public static Status status(String word) {
        return switch (word) {
            case "en attente" -> Status.builder().run(Status.Run.PENDING).build();
            case "en cours" -> Status.builder().run(Status.Run.RUNNING).build();
            case "terminée avec succès" -> Status.builder().run(Status.Run.DONE).exit(Status.Exit.SUCCESS).build();
            case "terminée en échec" -> Status.builder().run(Status.Run.DONE).exit(Status.Exit.FAILURE).build();
            default -> throw new IllegalArgumentException("statut de tâche inconnu : " + word);
        };
    }

    public static TaskLogCreation.Level level(String word) {
        return switch (word) {
            case "d'information" -> TaskLogCreation.Level.INFO;
            case "d'avertissement" -> TaskLogCreation.Level.WARN;
            case "d'erreur" -> TaskLogCreation.Level.ERROR;
            default -> throw new IllegalArgumentException("niveau de journal inconnu : " + word);
        };
    }

    public static TaskNotification.Type notification(String word) {
        return switch (word) {
            case "création" -> TaskNotification.Type.CREATED;
            case "changement de statut" -> TaskNotification.Type.STATUS_CHANGE;
            case "nouveau journal" -> TaskNotification.Type.LOG_APPENDED;
            default -> throw new IllegalArgumentException("notification inconnue : " + word);
        };
    }
}
