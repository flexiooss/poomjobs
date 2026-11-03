Feature: Avancement d'une tâche
  L'exécutant d'une tâche signale son avancement : en cours, puis terminée avec succès ou en échec.

  # source: UpdateTaskStatus#apply
  @confirmed
  Scenario: Passer une tâche en cours note son démarrage
    Given une tâche en attente
    When je passe la tâche en cours
    Then la demande est acceptée
    And la tâche est en cours
    And la tâche a été démarrée à l'instant (heure UTC)

  # source: UpdateTaskStatus#apply (issue absente → SUCCESS)
  @confirmed
  Scenario Outline: Terminer une tâche note sa fin et son issue
    Given une tâche en cours
    When je termine la tâche <issue>
    Then la tâche est <statut>
    And la tâche a été terminée à l'instant (heure UTC)

    Examples:
      | issue                     | statut               |
      | sans préciser son issue   | terminée avec succès |
      | avec succès               | terminée avec succès |
      | en échec                  | terminée en échec    |

  # source: UpdateTaskStatus#apply
  @confirmed
  Scenario: Une tâche en attente peut être terminée directement
    Given une tâche en attente
    When je termine la tâche sans préciser son issue
    Then la tâche est terminée avec succès

  # source: UpdateTaskStatus#apply
  # corrigé: F2 (docs/tests/findings.md)
  @confirmed
  Scenario: Repasser en cours une tâche déjà en cours garde sa date de démarrage
    Given une tâche en cours démarrée le 2026-01-05 à 10:00
    When je passe la tâche en cours
    Then la demande est acceptée
    And la tâche est en cours
    And la tâche a été démarrée le 2026-01-05 à 10:00

  # source: UpdateTaskStatus#apply (statut DONE → 400)
  @confirmed
  Scenario Outline: L'avancement d'une tâche terminée ne change plus
    Given une tâche <statut>
    When je <action>
    Then la demande est refusée car la tâche est terminée
    And la tâche est <statut>

    Examples:
      | statut               | action                       |
      | terminée avec succès | passe la tâche en cours      |
      | terminée en échec    | passe la tâche en cours      |
      | terminée en échec    | termine la tâche avec succès |

  # source: UpdateTaskStatus#apply → AbstractTaskHandler#notifyCallback
  @confirmed
  Scenario Outline: Le point de rappel est notifié de l'avancement de la tâche
    Given une tâche en cours suivie par un point de rappel
    When je termine la tâche <issue>
    Then le point de rappel reçoit une notification de changement de statut de la tâche <statut>
    And la notification porte la tâche

    Examples:
      | issue       | statut               |
      | avec succès | terminée avec succès |
      | en échec    | terminée en échec    |
