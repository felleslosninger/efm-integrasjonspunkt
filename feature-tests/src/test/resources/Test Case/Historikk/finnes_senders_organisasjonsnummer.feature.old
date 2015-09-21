Feature: Finne ut om sender kan sende melding

  Scenario Outline: sjekke om sender kan sende melding
    Given sender med organisasjonsnummer <organisasjonsnummer>
    When  vi sjekker sender sitt orgnummer
    Then  skal vi får <resultat> i svar
	
  Examples: values
    | organisasjonsnummer | resultat |
    | 974720760           | sann     |
    | 123456789           | usann    |