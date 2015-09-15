Feature: Kan ikke sende melding - Orgnummer finnes ikke

  Scenario Outline: sjekke om mottaker orgnummer finnes i adresseregister
    Given en mottakende organisasjon med organisasjonsnummer ?????????
    When vi sjekker om mottaker finnes i adresseregister
    Then skal vi få false i svar
  