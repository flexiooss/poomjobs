Feature: Journaux d'une tâche
  L'exécutant d'une tâche y ajoute des journaux (information, avertissement, erreur) que l'on consulte pour
  suivre son déroulement.

  # source: CreateTaskLog#apply
  @confirmed
  Scenario: Ajouter un journal à une tâche
    Given la tâche "A" en cours
    When j'ajoute le journal d'information "calcul démarré" à la tâche
    Then la demande est acceptée
    And la tâche "A" a les journaux :
      | calcul démarré |

  # source: BrowseTaskLogs#apply (filtre taskId == …)
  @confirmed
  Scenario: Les journaux d'une tâche ne montrent que les siens
    Given la tâche "A" en cours
    And la tâche "B" en cours
    And le journal "journal de A" a été ajouté à la tâche "A"
    And le journal "journal de B" a été ajouté à la tâche "B"
    When je consulte les journaux de la tâche "A"
    Then je reçois les journaux :
      | journal de A |

  # source: BrowseTaskLogs#apply (sans tri demandé : ordre du dépôt)
  @confirmed
  Scenario: Les journaux d'une tâche se lisent dans l'ordre où ils ont été ajoutés
    Given la tâche "A" en cours
    And le journal "premier" a été ajouté à la tâche "A"
    And le journal "deuxième" a été ajouté à la tâche "A"
    And le journal "troisième" a été ajouté à la tâche "A"
    When je consulte les journaux de la tâche "A"
    Then je reçois les journaux :
      | premier   |
      | deuxième  |
      | troisième |

  # source: BrowseTaskLogs#query (filtre de la requête combiné au filtre taskId)
  @confirmed
  Scenario: Les journaux d'une tâche se restreignent à un niveau
    Given la tâche "A" en cours
    And le journal d'information "étape 1" a été ajouté à la tâche "A"
    And le journal d'erreur "connexion perdue" a été ajouté à la tâche "A"
    And le journal d'avertissement "reprise" a été ajouté à la tâche "A"
    When je consulte les journaux d'erreur de la tâche "A"
    Then je reçois les journaux :
      | connexion perdue |

  # source: CreateTaskLog#apply
  # écart: F3 (docs/tests/findings.md)
  @known-bug
  Scenario: Une tâche terminée accepte encore des journaux
    Given la tâche "A" terminée avec succès
    When j'ajoute le journal d'information "trop tard" à la tâche
    Then la demande est acceptée
    And la tâche "A" a les journaux :
      | trop tard |

  # source: BrowseTaskLogs#apply (filtre sur taskId, sans vérifier que la tâche existe)
  # écart: F4 (docs/tests/findings.md)
  @known-bug
  Scenario: Consulter les journaux d'une tâche inexistante renvoie une liste vide
    When je consulte les journaux d'une tâche inexistante
    Then la demande est acceptée
    And je ne reçois aucun journal

  # source: CreateTaskLog#apply → AbstractTaskHandler#notifyCallback
  @confirmed
  Scenario: Le point de rappel est notifié d'un nouveau journal
    Given une tâche en cours suivie par un point de rappel
    When j'ajoute le journal d'avertissement "à mi-chemin" à la tâche
    Then le point de rappel reçoit une notification de nouveau journal de la tâche en cours
    And la notification porte le journal "à mi-chemin"

  # source: CreateTaskLog#apply (taskLogs() vide : journal non conservé, 201 quand même)
  @confirmed
  Scenario: Sans conservation des journaux, un journal ajouté est accepté mais pas conservé
    Given le point d'entrée ne conserve pas les journaux
    And la tâche "A" en cours
    When j'ajoute le journal d'information "perdu" à la tâche
    Then la demande est acceptée
    And la tâche "A" n'a aucun journal

  # source: CreateTaskLog#apply → AbstractTaskHandler#notifyCallback (notification même sans dépôt)
  @confirmed
  Scenario: Sans conservation des journaux, le point de rappel est quand même notifié d'un nouveau journal
    Given le point d'entrée ne conserve pas les journaux
    And une tâche en cours suivie par un point de rappel
    When j'ajoute le journal d'information "transmis" à la tâche
    Then le point de rappel reçoit une notification de nouveau journal de la tâche en cours

  # source: BrowseTaskLogs#apply (taskLogs() vide → 405 COLLECTION_BROWSING_NOT_ALLOWED)
  @confirmed
  Scenario: Sans conservation des journaux, leur consultation n'est pas permise
    Given le point d'entrée ne conserve pas les journaux
    And la tâche "A" en cours
    When je consulte les journaux de la tâche "A"
    Then la consultation des journaux n'est pas permise
