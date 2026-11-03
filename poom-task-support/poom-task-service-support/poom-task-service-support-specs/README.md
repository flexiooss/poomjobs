# poom-task-service-support-specs

Règles métier des points d'entrée de tâches (handlers génériques de `poom-task-service-support-handlers`) écrites en
scénarios Gherkin (Cucumber-JVM), **réutilisables** : chaque implémentation de `TaskEntryPointAdapter` (en mémoire,
Mongo, Flexio…) exécute la même suite pour prouver qu'elle respecte les règles.

Les règles sont dans `src/main/resources/features/tasks/`, leur état et leur vocabulaire dans `docs/tests/` à la racine
du repo.

## Exécuter la suite sur un adaptateur

1. Dépendance, en scope `test` (elle apporte Cucumber, la suite JUnit Platform et hamcrest) :

   ```xml
   <dependency>
       <groupId>org.codingmatters.tasks</groupId>
       <artifactId>poom-task-service-support-specs</artifactId>
       <scope>test</scope>
   </dependency>
   ```

   Des tests JUnit 4 dans le même module passent alors par `junit-vintage-engine` (scope `test`).

2. Implémenter `TaskEntryPointsUnderTest` :
   - `adapter(account)` : l'adaptateur du compte, avec un dépôt de journaux ; `callbackRequester(url)` doit poster
     réellement sur l'URL reçue (serveur HTTP local de la suite) ;
   - `reset()` : vider tâches et journaux de tous les comptes (appelé avant chaque scénario).

   La déclarer dans `src/test/resources/META-INF/services/org.codingmatters.tasks.support.specs.TaskEntryPointsUnderTest`.

3. Le runner :

   ```java
   @Suite
   @IncludeEngines("cucumber")
   @SelectClasspathResource("features/tasks")
   @ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "org.codingmatters.tasks.support.specs.steps")
   @ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, summary")
   public class RunTaskSpecsTest {
   }
   ```

Exemple : `poom-task-service-support-demo-handlers` (`InMemoryTaskEntryPoints`, `RunTaskSpecsTest`).

## Ce que la suite construit elle-même

- les sept handlers génériques, appelés directement (sans HTTP) autour de l'adaptateur fourni ; les réglages propres à
  un scénario (taille de page, absence de journaux, arguments additionnels du job) décorent cet adaptateur ;
- le registre de jobs simulé (`JobRegistryDouble` : accepte, refuse ou est injoignable) ;
- le point de rappel (`CallbackReceiver` : serveur Undertow local qui enregistre les notifications).

Le routage HTTP d'un service (quel endpoint branche quels handlers, quel nom de job, quelle taille de page) reste à
vérifier dans le service lui-même, par quelques scénarios de câblage.
