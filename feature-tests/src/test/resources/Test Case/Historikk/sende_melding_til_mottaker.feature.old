Feature: Send melding fra Sak- og arkivkløsnigen sitt perspektiv

  Scenario: Avlevering til Altinn
    Given En NoArk melding
    And Mottaker med organisasjonsnummer 123456789 som finnes i adresseregister
    When meldingen mottas av integrasjonspunktet
    Then skal meldingen sendes videresendes til altinn
    And vi skal få bekreftelse på at melding er videresendt