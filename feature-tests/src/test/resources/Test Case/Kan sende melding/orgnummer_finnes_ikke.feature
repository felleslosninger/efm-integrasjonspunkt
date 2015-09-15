Feature: Kan sende melding

  Scenario Outline: mottaker sitt orgnummer finnes ikke i adresseregister.
    Given mottaker med organisasjonsnummer <organisasjonsnummer>
    When  organisasjonsnummer ikke finnes
    Then  skal vi får <resultat> i svar
    Examples: values
      | organisasjonsnummer | resultat |
      | 123456789           | usann     |

  