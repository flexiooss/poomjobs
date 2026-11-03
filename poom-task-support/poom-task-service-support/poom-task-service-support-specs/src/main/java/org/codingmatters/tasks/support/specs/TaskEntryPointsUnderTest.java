package org.codingmatters.tasks.support.specs;

import org.codingmatters.tasks.support.api.TaskEntryPointAdapter;

/**
 * Point d'extension des règles des tâches : l'adaptateur de point d'entrée à éprouver, fourni par le module qui
 * exécute la suite et déclaré dans {@code META-INF/services/org.codingmatters.tasks.support.specs.TaskEntryPointsUnderTest}
 * de ses ressources de test.
 * <p>
 * La suite construit elle-même les handlers génériques autour de cet adaptateur. Elle insère et lit les tâches et
 * les journaux par ses dépôts, compare le job soumis à {@link TaskEntryPointAdapter#jobSpecFor} et
 * {@link TaskEntryPointAdapter#jobAccount}, et attend que {@link TaskEntryPointAdapter#callbackRequester} poste
 * réellement sur l'URL de rappel reçue (serveur HTTP local de la suite).
 */
public interface TaskEntryPointsUnderTest {

    /** Adaptateur du point d'entrée de tâches de ce compte ; ses dépôts doivent offrir les journaux. */
    TaskEntryPointAdapter adapter(String account);

    /** Vide les tâches et les journaux de tous les comptes : appelé avant chaque scénario. */
    void reset();
}
