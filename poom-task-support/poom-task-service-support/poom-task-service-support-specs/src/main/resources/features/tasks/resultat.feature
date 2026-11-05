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

  # source: ReplaceTaskResult#apply
  # corrigé: F5 (docs/tests/findings.md)
  @confirmed
  Scenario Outline: Le résultat d'une tâche terminée ne peut plus être remplacé
    Given une tâche <statut>
    And un résultat a été déposé sur la tâche
    When je dépose un autre résultat sur la tâche
    Then la demande est refusée car la tâche est terminée
    And la tâche porte le résultat déposé

    Examples:
      | statut               |
      | terminée avec succès |
      | terminée en échec    |

  # source: ReplaceTaskResult#apply → AbstractTaskHandler#notifyCallback
  # corrigé: F6 (docs/tests/findings.md)
  @confirmed
  Scenario: Déposer un résultat notifie le point de rappel
    Given une tâche en cours suivie par un point de rappel
    When je dépose un résultat sur la tâche
    Then le point de rappel reçoit une notification de changement de statut de la tâche en cours
    And la notification porte la tâche
    And la notification porte le résultat déposé
