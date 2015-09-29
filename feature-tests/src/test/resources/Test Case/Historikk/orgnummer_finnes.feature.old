Feature: Kan sende melding

  Scenario Outline:  mottaker sitt orgnummer finnes i adresseregister.
    Given mottaker med organisasjonsnummer <organisasjonsnummer>
    When  mottaker finnes i adresseregister
    Then  skal vi få <resultat> i svar
  Examples: values
    | organisasjonsnummer | resultat |
    | 974720760           | sann     |