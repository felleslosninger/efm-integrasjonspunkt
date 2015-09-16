Feature: Send melding fra Sak- og arkivkløsnigen sitt perspektiv

  Scenario: Sende melding til mottaker som finnes i adresseregister
    Given En NoArk melding
    And Mottaker med organisasjonsnummer 123456789 som finnes i adresseregister
    When meldingen mottas
    Then skal meldingen sendes videresendes til mottaker
    And vi skal få bekreftelse på at melding er videresendt