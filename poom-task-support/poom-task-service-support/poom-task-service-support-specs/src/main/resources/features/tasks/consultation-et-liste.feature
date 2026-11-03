Feature: Consultation et liste des tâches
  On consulte une tâche pour suivre son avancement, ou on liste les tâches du point d'entrée.

  # source: RetrieveTask#apply
  @as-is @new-step
  Scenario: Consulter une tâche
    Given une tâche en cours
    When je consulte la tâche
    Then je reçois la tâche

  # source: RetrieveTask / UpdateTaskStatus / CreateTaskLog / ReplaceTaskResult #apply (404)
  @as-is @new-step
  Scenario Outline: Une action sur une tâche inexistante est refusée
    When je <action> une tâche inexistante
    Then la demande est refusée car la tâche est introuvable

    Examples:
      | action                  |
      | consulte                |
      | passe en cours          |
      | termine                 |
      | journalise              |
      | dépose un résultat pour |

  # source: BrowseTasks#apply
  @as-is @new-step
  Scenario: Lister les tâches renvoie toutes les tâches du point d'entrée
    Given la tâche "A" en attente
    And la tâche "B" en cours
    And la tâche "C" terminée en échec
    When je liste les tâches
    Then je reçois les tâches "A", "B" et "C"

  # source: BrowseTasks#query (filtre de la requête)
  @as-is @new-step
  Scenario: La liste des tâches se restreint à un statut
    Given la tâche "A" en attente
    And la tâche "B" en cours
    And la tâche "C" en cours
    When je liste les tâches en cours
    Then je reçois les tâches "B" et "C"

  # source: BrowseTasks#query (tri de la requête)
  @as-is @new-step
  Scenario: La liste des tâches se trie des plus récentes aux plus anciennes
    Given la tâche "A" en attente créée le 2026-01-05 à 09:30
    And la tâche "B" en attente créée le 2026-01-07 à 09:30
    And la tâche "C" en attente créée le 2026-01-06 à 09:30
    When je liste les tâches des plus récentes aux plus anciennes
    Then je reçois, dans cet ordre, les tâches "B", "C" et "A"

  # source: BrowseTasks#apply → Rfc7233Pager (page par défaut = taille maximale du point d'entrée)
  @as-is @new-step
  Scenario: Sans précision, la liste est servie par pages de la taille maximale du point d'entrée
    Given le point d'entrée liste au plus 3 tâches par page
    And 4 tâches en attente
    When je liste les tâches
    Then je reçois 3 tâches
    And la liste est incomplète

  # source: BrowseTasks#apply → Range#fromRequestedRange (plage tronquée à la taille maximale)
  @as-is @new-step
  Scenario: Une page demandée plus grande que la taille maximale est réduite à cette taille
    Given le point d'entrée liste au plus 3 tâches par page
    And 5 tâches en attente
    When je liste les tâches de la position 0 à 4
    Then je reçois 3 tâches
    And la liste est incomplète

  # source: BrowseTasks#apply → Range#fromRequestedRange (expression illisible, début après la fin → 416)
  @as-is @new-step
  Scenario Outline: Une page demandée mal formée est refusée
    Given 2 tâches en attente
    When je liste les tâches de la page "<page>"
    Then la page demandée est refusée

    Examples:
      | page |
      | 3-1  |
      | tout |
