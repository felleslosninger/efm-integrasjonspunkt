Feature: Kan ikke sende melding - orgnummer ikke gyldig

  Scenario Outline: validerer mottaker orgnummer
    Given en mottakende organisasjon med organisasjonsnummer ????????? finnes i adresseregister
    When vi sjekker om organisasjonsnummer er gyldig 
    Then skal vi få false i svar
  