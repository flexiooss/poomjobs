# Catalogue des steps

> Généré par `steps_catalog.py` depuis la javadoc des step definitions — ne pas éditer à la main.

## Given

| Step | Effet métier | Usages | Définition |
| --- | --- | --- | --- |
| `la tâche {string} {statutTache}` | Une tâche de ce statut existe, désignée par ce nom dans le scénario. | 14 | `TaskSteps#existingNamedTask` |
| `la tâche {string} {statutTache} créée le {moment}` | Une tâche de ce statut, désignée par ce nom, a été créée à ce moment. | 3 | `TaskSteps#existingNamedTaskCreatedAt` |
| `le journal {niveauJournal} {string} a été ajouté à la tâche {string}` | Ce journal de ce niveau a déjà été ajouté à la tâche de ce nom. | 3 | `TaskSteps#loggedTaskAtLevel` |
| `le journal {string} a été ajouté à la tâche {string}` | Ce journal d'information a déjà été ajouté à la tâche de ce nom. | 5 | `TaskSteps#loggedTask` |
| `le point d'entrée ajoute les arguments {noms} aux jobs` | La spécification de job du point d'entrée porte ces arguments additionnels. | 1 | `TaskSteps#additionalJobArguments` |
| `le point d'entrée liste au plus {int} tâches par page` | Le point d'entrée sert ses listes par pages d'au plus ce nombre d'éléments. | 2 | `TaskSteps#maxPageSize` |
| `le point d'entrée ne conserve pas les journaux` | Le point d'entrée n'a pas de dépôt de journaux. | 3 | `TaskSteps#noLogRepository` |
| `le point de rappel répond en erreur` | Le point de rappel des tâches répond par une erreur à toute notification. | 1 | `TaskSteps#callbackFails` |
| `le registre de jobs est injoignable` | Le registre de jobs ne répond pas (erreur d'entrée/sortie à l'appel). | 1 | `TaskSteps#jobRegistryUnreachable` |
| `le registre de jobs refuse les jobs` | Le registre de jobs refuse toute soumission de job. | 2 | `TaskSteps#jobRegistryRefuses` |
| `un résultat a été déposé sur la tâche` | Un premier résultat a déjà été déposé sur la tâche du scénario. | 3 | `TaskSteps#resultDeposited` |
| `une tâche {statutTache}` | Une tâche de ce statut existe ; elle devient la tâche du scénario. | 15 | `TaskSteps#existingTask` |
| `une tâche {statutTache} suivie par un point de rappel` | Une tâche de ce statut existe, suivie par le point de rappel du scénario. | 5 | `TaskSteps#existingFollowedTask` |
| `{int} tâches en attente` | Ce nombre de tâches en attente existe. | 4 | `TaskSteps#manyTasks` |

## When

| Step | Effet métier | Usages | Définition |
| --- | --- | --- | --- |
| `j'ajoute le journal {niveauJournal} {string} à la tâche` | L'exécutant ajoute ce journal de ce niveau à la tâche du scénario. | 5 | `TaskSteps#logTask` |
| `je consulte la tâche` | Le demandeur consulte la tâche du scénario. | 1 | `TaskSteps#retrieveTask` |
| `je consulte les journaux d'une tâche inexistante` | Le demandeur consulte les journaux d'une tâche qui n'existe pas. | 1 | `TaskSteps#readLogsOfUnknownTask` |
| `je consulte les journaux de la tâche {string}` | Le demandeur consulte tous les journaux de la tâche de ce nom. | 3 | `TaskSteps#readLogs` |
| `je consulte les journaux {niveauJournal} de la tâche {string}` | Le demandeur consulte les journaux de ce niveau de la tâche de ce nom. | 1 | `TaskSteps#readLogsAtLevel` |
| `je demande une tâche` | Le demandeur demande une tâche, avec la demande type, sans point de rappel. | 6 | `TaskSteps#createTask` |
| `je demande une tâche suivie par un point de rappel` | Le demandeur demande une tâche en donnant un point de rappel à notifier. | 3 | `TaskSteps#createFollowedTask` |
| `je dépose un autre résultat sur la tâche` | L'exécutant dépose sur la tâche du scénario un résultat différent du précédent. | 3 | `TaskSteps#depositOtherResult` |
| `je dépose un résultat sur la tâche` | L'exécutant dépose un résultat sur la tâche du scénario. | 3 | `TaskSteps#depositResult` |
| `je liste les tâches` | Le demandeur liste les tâches, sans filtre, tri ni page. | 2 | `TaskSteps#listTasks` |
| `je liste les tâches de la page {string}` | Le demandeur liste les tâches en demandant cette page, écrite telle quelle. | 2 | `TaskSteps#listPage` |
| `je liste les tâches de la position {int} à {int}` | Le demandeur liste les tâches de cette position à cette position (incluses). | 1 | `TaskSteps#listRange` |
| `je liste les tâches des plus récentes aux plus anciennes` | Le demandeur liste les tâches triées par date de création décroissante. | 1 | `TaskSteps#listNewestFirst` |
| `je liste les tâches {statutTache}` | Le demandeur liste les tâches de ce statut (avancement seul). | 1 | `TaskSteps#listTasksWithStatus` |
| `je passe la tâche en cours` | L'exécutant signale la tâche du scénario en cours. | 4 | `TaskSteps#startTask` |
| `je termine la tâche {issueTache}` | L'exécutant signale la tâche du scénario terminée, avec cette issue ou sans la préciser. | 7 | `TaskSteps#finishTask` |
| `je {operationTache} une tâche inexistante` | Le demandeur ou l'exécutant applique cette action à une tâche qui n'existe pas. | 5 | `TaskSteps#onUnknownTask` |

## Then

| Step | Effet métier | Usages | Définition |
| --- | --- | --- | --- |
| `je ne reçois aucun journal` | La dernière lecture de journaux n'en a renvoyé aucun. | 1 | `TaskSteps#receivedNoLog` |
| `je reçois la tâche` | La dernière action a renvoyé la tâche du scénario, avec son statut enregistré. | 2 | `TaskSteps#receivedTask` |
| `je reçois les journaux :` | Les derniers journaux lus sont exactement ceux-ci, dans cet ordre. | 3 | `TaskSteps#receivedLogs` |
| `je reçois les tâches {noms}` | La dernière page reçue contient exactement les tâches de ces noms, dans n'importe quel ordre. | 2 | `TaskSteps#receivedTasks` |
| `je reçois {int} tâches` | La dernière page reçue contient ce nombre de tâches. | 2 | `TaskSteps#receivedTaskCount` |
| `je reçois, dans cet ordre, les tâches {noms}` | La dernière page reçue contient exactement les tâches de ces noms, dans cet ordre. | 1 | `TaskSteps#receivedTasksInOrder` |
| `la consultation des journaux n'est pas permise` | La consultation des journaux est refusée : le point d'entrée ne les parcourt pas. | 1 | `TaskSteps#logBrowsingNotAllowed` |
| `la demande est acceptée` | La dernière action est acceptée. | 10 | `TaskSteps#accepted` |
| `la demande est refusée car la tâche est introuvable` | La dernière action est refusée parce que la tâche visée n'existe pas. | 5 | `TaskSteps#refusedTaskNotFound` |
| `la demande est refusée car la tâche est terminée` | La dernière action est refusée parce que la tâche visée est déjà terminée. | 3 | `TaskSteps#refusedTaskDone` |
| `la demande échoue sur une erreur inattendue` | La dernière action échoue sur une erreur inattendue. | 2 | `TaskSteps#failedUnexpectedly` |
| `la liste est incomplète` | La dernière page reçue n'est pas la fin de la liste. | 2 | `TaskSteps#listIsPartial` |
| `la notification porte la tâche` | La notification reçue porte la tâche du scénario, avec son statut enregistré. | 3 | `TaskSteps#notificationCarriesTask` |
| `la notification porte le journal {string}` | La notification reçue porte ce journal, rattaché à la tâche du scénario. | 1 | `TaskSteps#notificationCarriesLog` |
| `la page demandée est refusée` | La page demandée n'est pas une plage valide. | 2 | `TaskSteps#pageRefused` |
| `la tâche a été {momentTache} à l'instant \(heure UTC)` | La tâche du scénario a été créée, démarrée ou terminée à l'instant présent, en heure UTC. | 7 | `TaskSteps#taskMomentIsNow` |
| `la tâche conserve sa demande` | La tâche du scénario a pour paramètres ceux de la demande envoyée. | 1 | `TaskSteps#taskKeepsRequest` |
| `la tâche est {statutTache}` | La tâche du scénario est enregistrée avec ce statut. | 14 | `TaskSteps#taskHasStatus` |
| `la tâche porte le dernier résultat déposé` | La tâche du scénario porte le dernier résultat déposé, et lui seul. | 3 | `TaskSteps#taskCarriesLastResult` |
| `la tâche porte le résultat déposé` | La tâche du scénario porte le premier résultat déposé. | 2 | `TaskSteps#taskCarriesResult` |
| `la tâche porte son identifiant` | La tâche du scénario porte, comme identifiant, celui sous lequel elle est enregistrée. | 1 | `TaskSteps#taskCarriesItsId` |
| `la tâche {string} a les journaux :` | La tâche de ce nom a exactement ces journaux conservés, dans cet ordre. | 2 | `TaskSteps#taskHasLogs` |
| `la tâche {string} n'a aucun journal` | La tâche de ce nom n'a aucun journal conservé. | 1 | `TaskSteps#taskHasNoLog` |
| `le point de rappel ne reçoit aucune notification` | Le point de rappel n'a reçu aucune notification. | 2 | `TaskSteps#noCallback` |
| `le point de rappel reçoit une notification de {notificationTache} de la tâche {statutTache}` | Le point de rappel a reçu une seule notification de ce type, signalant ce statut de la tâche (en-têtes d'avancement et d'issue). | 5 | `TaskSteps#callbackNotified` |
| `un job est soumis pour la tâche` | Un seul job a été soumis au registre pour la tâche du scénario, tel que le décrit le point d'entrée : compte du job, catégorie et nom, et pour arguments l'identifiant de la tâche, l'URL des tâches et les arguments additionnels. | 2 | `TaskSteps#jobSubmitted` |
