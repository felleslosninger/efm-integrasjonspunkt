Feature: Kan sende melding

  Scenario Outline: mottaker orgnummer er ikke gyldig
    Given  mottaker med organisasjonsnummer <organisasjonsnummer>
    When   organisasjonsnummer er ikkje gyldig
    Then   skal vi får <resultat> i svar
    Examples: values
      | organisasjonsnummer | resultat |
      | ?????????           | usann    |

 #Business Need: Trenger et ugyldig orgnummer, et som ikkje validerer