Feature: Résultat d'une tâche
  L'exécutant dépose le résultat de la tâche, que le demandeur lit sur la tâche.

  # source: ReplaceTaskResult#apply
  @confirmed
  Scenario: Déposer le résultat d'une tâche
    Given une tâche en cours
    When je dépose un résultat sur la tâche
    Then la demande est acceptée
    And la tâche porte le résultat déposé

  # source: ReplaceTaskResult#apply (withResults : remplacement complet)
  @confirmed
  Scenario: Déposer un nouveau résultat remplace le précédent
    Given une tâche en cours
    And un résultat a été déposé sur la tâche
    When je dépose un autre résultat sur la tâche
    Then la tâche porte le dernier résultat déposé

  # source: ReplaceTaskResult#apply
  @confirmed
  Scenario: Déposer un résultat ne termine pas la tâche
    Given une tâche en attente
    When je dépose un résultat sur la tâche
    Then la tâche porte le résultat déposé
    And la tâche est en attente

  # source: ReplaceTaskResult#apply (aucune vérification du statut)
  # écart: F5 (docs/tests/findings.md)
  @known-bug
  Scenario Outline: Le résultat d'une tâche terminée peut encore être remplacé
    Given une tâche <statut>
    And un résultat a été déposé sur la tâche
    When je dépose un autre résultat sur la tâche
    Then la demande est acceptée
    And la tâche porte le dernier résultat déposé

    Examples:
      | statut               |
      | terminée avec succès |
      | terminée en échec    |

  # source: ReplaceTaskResult#apply (aucun appel à notifyCallback)
  # écart: F6 (docs/tests/findings.md)
  @known-bug
  Scenario: Déposer un résultat ne notifie pas le point de rappel
    Given une tâche en cours suivie par un point de rappel
    When je dépose un résultat sur la tâche
    Then le point de rappel ne reçoit aucune notification
