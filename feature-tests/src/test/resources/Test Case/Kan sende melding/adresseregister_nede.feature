Feature: Kan sende melding

  Scenario Outline:  Adresseregister nede.
    Given mottaker med organisasjonsnummer <organisasjonsnummer>
    When  adresseregister svarer ikkje
    Then  skal vi får <resultat> i svar
    Examples: values
      | organisasjonsnummer | resultat |
      | 974720760           | usann     |
