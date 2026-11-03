Feature: Demande d'une tâche
  Un traitement long est demandé en tâche : le point d'entrée enregistre la tâche et confie son exécution au
  registre de jobs ; un point de rappel peut suivre la tâche.

  # source: CreateTask#apply
  @confirmed
  Scenario: Demander une tâche la crée en attente avec sa demande
    When je demande une tâche
    Then la demande est acceptée
    And la tâche est en attente
    And la tâche porte son identifiant
    And la tâche conserve sa demande
    And la tâche a été créée à l'instant (heure UTC)

  # source: CreateTask#apply (status201 : payload = tâche enregistrée)
  @confirmed
  Scenario: La réponse à une demande de tâche est la tâche créée
    When je demande une tâche
    Then je reçois la tâche

  # source: CreateTask#apply → TaskEntryPointAdapter#jobSpecFor / #jobAccount
  @confirmed
  Scenario: Demander une tâche confie son exécution au registre de jobs
    When je demande une tâche
    Then un job est soumis pour la tâche

  # source: CreateTask#apply (JobSpec#additionalArgs ajoutés après l'identifiant et l'URL des tâches)
  @confirmed
  Scenario: Les arguments additionnels du point d'entrée sont transmis au job
    Given le point d'entrée ajoute les arguments "fr" et "rapide" aux jobs
    When je demande une tâche
    Then un job est soumis pour la tâche

  # source: CreateTask#apply (réponse du registre autre que 201)
  @confirmed
  Scenario: Une tâche refusée par le registre de jobs est terminée en échec
    Given le registre de jobs refuse les jobs
    When je demande une tâche
    Then la demande échoue sur une erreur inattendue
    And la tâche est terminée en échec
    And la tâche a été terminée à l'instant (heure UTC)

  # source: CreateTask#apply (IOException du client du registre)
  # écart: F1 (docs/tests/findings.md)
  @known-bug
  Scenario: Une tâche dont le registre de jobs est injoignable reste en attente
    Given le registre de jobs est injoignable
    When je demande une tâche
    Then la demande échoue sur une erreur inattendue
    And la tâche est en attente

  # source: CreateTask#apply → AbstractTaskHandler#notifyCallback
  @confirmed
  Scenario: Le point de rappel est notifié de la création de la tâche
    When je demande une tâche suivie par un point de rappel
    Then le point de rappel reçoit une notification de création de la tâche en attente
    And la notification porte la tâche

  # source: CreateTask#apply (retour en erreur avant la notification)
  @confirmed
  Scenario: Une tâche refusée par le registre de jobs n'est pas notifiée
    Given le registre de jobs refuse les jobs
    When je demande une tâche suivie par un point de rappel
    Then le point de rappel ne reçoit aucune notification

  # source: AbstractTaskHandler#notifyCallback (réponse autre que 204 seulement journalisée)
  @confirmed
  Scenario: Un point de rappel en erreur n'empêche pas la création de la tâche
    Given le point de rappel répond en erreur
    When je demande une tâche suivie par un point de rappel
    Then la demande est acceptée
    And la tâche est en attente
