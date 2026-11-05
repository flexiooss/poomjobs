# État de la démarche Gherkin

## Configuration
- Module des règles (réutilisable) : `poom-task-support/poom-task-service-support/poom-task-service-support-specs`
  — features dans `src/main/resources/features/tasks/`, steps dans `src/main/java/…/specs/steps/`, couche domaine dans
  `src/main/java/…/specs/testdomain/`, doubles (`JobRegistryDouble`, `CallbackReceiver`) et point d'extension
  (`TaskEntryPointsUnderTest`, chargé par `ServiceLoader`) dans `src/main/java/…/specs/`. Mode d'emploi : `README.md` du module.
- Exécutions de la suite :
  | Adaptateur éprouvé | Module | Commande |
  | --- | --- | --- |
  | en mémoire (`InMemoryTaskEntryPoints`) | `poom-task-service-support-demo-handlers` | `mvn test -pl poom-task-support/poom-task-service-support/poom-task-service-support-demo/poom-task-service-support-demo-handlers` |
  | Flexio, Mongo (`FlexioTasksAdapterDefaultProvider`) | `flexio-services-commons/flexio-task-service-support` | à venir (étape 2) |
- Catalogue : `python3 -I <flexio-gherkin-rules>/scripts/steps_catalog.py poom-task-support/poom-task-service-support/poom-task-service-support-specs`
- Arbitre des questions de revue : nel (mainteneur du framework jobs / tâches).

## Ressources
| Ressource | Statut | Scénarios | Dernière boucle |
| --- | --- | --- | --- |
| tasks (handlers génériques) | reviewed | 37 scénarios / 51 exemples (@confirmed 37) — écarts F1–F6 corrigés | 2026-11-03 evolve |

## Mis de côté
- (aucun — boucle extract tasks du 2026-11-03)

## Remarques
- Revue tasks (2026-11-03, nel) : 32 règles confirmées, 6 bugs connus (F1–F6) gardés verts sur le comportement
  actuel. F5 tranché après coup : le dépôt sur une tâche en attente est voulu (scénario confirmé), le remplacement du
  résultat d'une tâche terminée est un bug → scénario ajouté, `@known-bug`, sans nouvelle step. Aucun attendu
  modifié. Les corrections de F1–F6 se font en `evolve` (scénario inversé, rouge avant code) ;
  F6 sans évolution de l'API. À l'étape 2, l'adaptateur Flexio hérite de ces tags tels quels.
- Extract tasks (2026-11-03) : migration des scénarios « framework » extraits dans flexio-demo (tâches de calcul de plus
  court chemin et de message de chat, 2026-11-03), généralisés à un point d'entrée quelconque, et complétés : liste
  (`BrowseTasks` : filtre, tri, pages), journaux (`BrowseTaskLogs` : ordre, niveau, tâche inexistante, absence de dépôt),
  arguments additionnels du job, registre injoignable, résultat remplacé / non notifié. Ce qui dépend de l'adaptateur
  Flexio (une base par compte, collections par type, URL des tâches) n'est pas ici : scénarios propres à
  `flexio-task-service-support`.
- Pagination : règles de `Rfc7233Pager` / `Range` (poom-services-support) exposées par les handlers, gardées au minimum
  (page par défaut = maximum, plage réduite au maximum, plage mal formée refusée).
- Tout vert au 1er run : 12 attendus faussés à la main (un par thème) → 12 scénarios en échec ; mutation du code de
  production (`CreateTask` : arguments additionnels et compte du job ignorés) → 2 scénarios en échec. Fichiers restaurés.
- Comparaison des tâches renvoyées ou notifiées : identifiant, statut et demande seulement (un dépôt Mongo tronque les
  dates à la milliseconde).

## Propositions en attente (consolidate)
- (aucune)

## Indicateurs (par boucle)
| Date | Mode | Ressource | Vert au 1er run | Mis de côté | Steps réutilisées / utilisées | Durée revue | Questions répétées |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 2026-11-03 | extract | tasks (adaptateur en mémoire) | 47/47 exemples (37/37 scénarios) | 0 | 0 / 57 steps distinctes (57 créées, la plupart reprises de flexio-demo et généralisées) | — | 0 |
| 2026-11-03 | review | tasks | 47/47 (aucun attendu modifié) | 0 | — | ~15 min (5 thèmes, 9 questions) | 0 |
| 2026-11-03 | evolve | tasks (F1–F6) | rouge attendu 8/8, puis 51/51 | 0 | 4 nouvelles / 60 steps | — | 0 |

## Évolutions
- 2026-11-03 — correction des écarts F1–F6 de la revue (nel) : 8 scénarios touchés (4 `@known-bug` inversés, 2 fusionnés
  en Outline avec la règle de refus du registre, 1 Outline étendu à l'injoignabilité — déjà vert, il verrouille
  « sans notification »), tous `@confirmed` avec `# corrigé: Fn`. Diff validé avant code ; rouge constaté : 8 exemples
  sur 51. Steps : 4 créées (`une tâche en cours démarrée le {moment}`, `la tâche {string} {statutTache} suivie par un
  point de rappel`, `la tâche a été {momentTache} le {moment}`, `la notification porte le résultat déposé`),
  1 supprimée (`je ne reçois aucun journal`) ; « un résultat a été déposé sur la tâche » passe par le dépôt (le
  handler refuse désormais une tâche terminée). Code : `CreateTask`, `UpdateTaskStatus`, `CreateTaskLog`,
  `BrowseTaskLogs`, `ReplaceTaskResult`, `AbstractTaskHandler#isDone` ; 7 tests unitaires ajoutés (dont
  `BrowseTaskLogsTest`).
