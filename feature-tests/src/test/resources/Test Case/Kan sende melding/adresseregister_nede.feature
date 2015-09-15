Feature: Kan ikke sende melding - Adresseregister nede

  Scenario Outline: adresseregister svarer ikkje når integrasjonspunktet henter org nummer
	Given en mottakende organisasjon med organisasjonsnummer ?????????
    When vi sjekker om mottaker finnes i adresseregister
    Then skal vi få feilmelding om at adresseregister ikke svarer
	
  