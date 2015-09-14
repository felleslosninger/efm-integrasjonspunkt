Feature: Finne ut om mottaker kan motta melding

  Scenario Outline: sjekke om mottaker kan motta meldinger
    Given en mottakende organisasjon med organisasjonsnummer <organisasjonsnummer>
    When vi sjekker om mottaker kan motta melding
    Then skal vi få <resultat> i svar
  Examples: values
    | organisasjonsnummer | resultat |
    | 974720760           | sann     |
    | 123456789           | usann    |