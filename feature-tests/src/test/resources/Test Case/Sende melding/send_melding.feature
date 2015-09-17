Feature: Størrelsen på en melding avgjør hvilken transportinfrastruktur som skal videreformidle meldingen
    	 For å finne størrelsen på meldingen som skal transporteres
		 Som integrasjonspunkt
		 Må eg kunne hene ut denne informasjonen å sende melding til riktig transportinfrastruktur
		 	

  Scenario: sender melding > 200 MB
	Given en melding på 201 MB
    When integrasjonspunkt mottar meldingen
    Then meldingen blir sent til Altinn
    And integrasjonspunkt returnerer Ok
	
  Scenario: sender melding < 200 MB
    Given en melding på 199 MB
    When størrelsen på meldingen blir sjekket
    Then meldingen blir sent til peppol

  Scenario: arkivsystem sender melding til et annet arkivsystem
    Given en melding
    When integrasjonspunkt mottar meldingen
    Then integrasjonspunkt sender melding videre til Altinn
    And integrasjonspunkt returnerer Ok

