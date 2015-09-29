Feature: Finne ut om sender kan sende melding
	     Organisasjonsnummer finnes, sjekker så at virksomhetssertifikat er gyldig

  Scenario Outline: sjekke om sender kan sende melding
    Given en sender med organisasjonsnummer <organisasjonsnummer> som har virksomhetssertifikat 
    When vi sjekker om sender sitt 
    Then skal vi få <resultat> i svar
	
  Examples: values
    | organisasjonsnummer | resultat |
    | 974720760           | sann     |
    | 123456789           | usann    |