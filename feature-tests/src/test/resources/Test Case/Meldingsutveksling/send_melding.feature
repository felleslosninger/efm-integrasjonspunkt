Feature: Ekspedere melding fra sak/arkivløsning som lagres i altinn sin formidlingstjeneste

  Scenario: Mottaker finnes ikke i adresseregister
    Given en melding med mottaker 1234
    And mottaker finnes ikke i adresseregisteret
    When vi skal sende melding
    Then Vi skal få beskjed om at mottaker ikke kan motta meldinger

  Scenario: Mottaker finnes i adresseregister
    Given en melding med mottaker 910077473
    And mottaker finnes i adresseregister
    When vi sender melding
    Then vi skal få svar om at melding har blitt formidlet


