# Connaissance du domaine

## Glossaire
- **Tâche** — traitement long demandé à un point d'entrée ; le point d'entrée l'enregistre et confie son **exécution**
  à un job du registre de jobs (poomjobs) ; l'**exécutant** (runner) signale son avancement, y ajoute des journaux et y
  dépose son résultat (extract 2026-11-03).
- **Point d'entrée de tâches** — collection de tâches d'un service, servie par les handlers génériques autour d'un
  `TaskEntryPointAdapter` (dépôts, spécification du job, compte du job, requêteur des rappels).
- **Demande** — paramètres donnés à la création de la tâche, conservés tels quels.
- **Avancement** — « en attente » → « en cours » → « terminée avec succès » / « terminée en échec » ; une tâche
  terminée ne change plus d'avancement.
- **Issue** — succès ou échec annoncé en terminant la tâche ; non précisée, c'est un succès.
- **Point de rappel** — URL donnée à la demande d'une tâche (« suivie par un point de rappel »), notifiée de sa création,
  de chaque changement d'avancement et de chaque nouveau journal.
- **Journal** — ligne datée d'un niveau (information, avertissement, erreur) ajoutée à une tâche par son exécutant.
- **Résultat** — objet déposé sur la tâche par son exécutant ; un nouveau dépôt remplace le précédent.
- **Registre de jobs** — service poomjobs qui reçoit les jobs ; il peut **refuser** un job (réponse en erreur) ou être
  **injoignable** (erreur d'entrée/sortie).
- **Conserver les journaux** — le point d'entrée a un dépôt de journaux ; sans lui, les journaux sont notifiés mais ni
  conservés ni consultables.
- Tâches sans libellé : une tâche est « la tâche » du scénario (la dernière demandée ou donnée), ou nommée par une
  lettre (« la tâche "A" ») quand le scénario en manipule plusieurs.

## Correspondances métier → technique
| Mot métier | Technique |
| --- | --- |
| demander une tâche | `CreateTask` (`TaskCollectionPostRequest` : `payload` = demande, `callbackUrl`) |
| « en attente » / « en cours » | `Status.Run.PENDING` / `RUNNING` |
| « terminée avec succès » / « en échec » | `Status.Run.DONE` + `Status.Exit.SUCCESS` / `FAILURE` |
| passer en cours / terminer | `UpdateTaskStatus` (`TaskStatusChange.Run.RUNNING` / `DONE`, `exit` facultatif) |
| date de création / démarrage / fin | `createdAt` / `startedAt` / `finishedAt`, en UTC (`UTC.now()`) |
| job soumis pour la tâche | `JobCollectionPostRequest` : `accountId` = `jobAccount()`, `category` / `name` = `jobSpecFor(task)`, arguments = [id de la tâche, `tasksUrl`, arguments additionnels] |
| registre qui refuse / injoignable | réponse autre que 201 / `IOException` du client |
| notification de création / de changement de statut / de nouveau journal | `TaskNotification.Type.CREATED` / `STATUS_CHANGE` / `LOG_APPENDED`, POST JSON sur l'URL de rappel, en-têtes `status` (avancement) et `result` (issue) |
| journal d'information / d'avertissement / d'erreur | `TaskLogCreation.Level.INFO` / `WARN` / `ERROR` |
| journaux d'un niveau | filtre `level == '<NIVEAU>'`, combiné à `taskId == '<id>'` |
| ne pas conserver les journaux | `TaskEntryPointAdapter#taskLogs()` vide → consultation 405 |
| déposer un résultat | `ReplaceTaskResult` (`TaskResultsPutRequest`, remplace `results`) |
| tâches d'un statut / des plus récentes aux plus anciennes | filtre `status.run == '<RUN>'` / tri `createdAt desc` |
| liste « incomplète » / page refusée | réponse `206` / `416` ; « au plus N tâches par page » = `maxPageSize` du handler, qui est aussi la page par défaut |
| tâche introuvable / tâche terminée (refus) | réponse `404` / `400` |

## Intentions expliquées
Revue des règles des tâches du 2026-11-03 (nel) :
- **Refus du registre sans notification** : la demande de tâche échoue de façon synchrone, l'appelant le sait déjà ;
  le point de rappel n'est notifié ni de la création ni de l'échec — voulu.
- **Registre injoignable** : la tâche doit être terminée en échec comme pour un refus (comportement actuel = bug F1).
- **Terminer une tâche en attente** est permis (exécutant qui échoue avant de démarrer, annulation) ; elle n'a alors
  pas de date de démarrage — voulu.
- **Relance d'une tâche en cours** : acceptée, mais la date de démarrage est la première (bug F2).
- **Une tâche terminée est figée** : ni avancement, ni journaux (bug F3), ni remplacement du résultat (bug F5).
- **Le résultat est indépendant de l'avancement** : on peut le déposer sur une tâche en attente ou en cours, ça ne la
  termine pas — voulu.
- **Toute action sur une tâche inexistante est refusée**, consultation des journaux comprise (bug F4).
- **Point d'entrée sans dépôt de journaux** : les journaux ne servent qu'à notifier le suiveur ; l'ajout reste accepté
  sans être conservé — voulu.
- **Dépôt d'un résultat** : doit notifier le suiveur par un changement de statut portant le résultat (bug F6).
