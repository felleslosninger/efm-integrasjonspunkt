Feature: Motta dokument fra SAK/ARKIV og videresende


  Scenario: Altinn nede
    Given SBD dokument skal videresendes til Altinn
    And Altinn er nede
    When vi sender dokumentet
    Then vi svarer med feilmelding tilbake til SAK/ARKIV

  Scenario: JournalpostId hentes fra Best/EDU melding formatert ihht, Fylkesmannen i Sogn og Fjordane  sitt sak/arkiv-system
    Given BEST/EDU dokument fra Fylkesmannen i Sogn og Fjordane
    When vi sender dokumentet
    Then JournalpostId skal finnes i SBD dokumentet

  Scenario: JournalpostId hentes fra public 360 formatert best/EDU
    Given BEST/EDU dokument fra public 360
    When vi sender dokumentet
    Then JournalpostId skal finnes i SBD dokumentet

  Scenario: Avsender eller mottaker har ugyldige virksomhetssertifikater
    Given avsender med ugyldig virksomhetssertifikat
    And mottaker med ugyldig virksomhetssertifikat
    When vi sender dokumentet
    Then dokumentet blir ikke sendt
    And feilmelding returneres til SAK/ARKIV system

  Scenario: Sender melding
    Given all is well
    When vi sender dokumentet
    Then dokumentet blir sendt videre
    And melding om at dokument er sendt videre returneres til SAK/ARKIV



































  Scenario: Mottaker finnes ikke i adresseregister
    Given   en melding med mottaker 1234
    And     mottaker finnes ikke i adresseregisteret
    When    vi skal sende melding
    Then    Vi skal få beskjed om at mottaker ikke kan motta meldinger

  Scenario: Mottaker finnes i adresseregister
    Given   en melding med mottaker 910077473
    And     mottaker finnes i adresseregister
    When    vi sender melding
    Then    vi skal få svar om at melding har blitt formidlet

  Scenario: mottaker orgnummer er ikke gyldig
    Given   en melding med mottaker 1234
    And     organisasjonsnummer ikkje er gyldig
    When    vi sender melding
    Then    Vi skal få beskjed om at mottaker ikke kan motta meldinger

  Scenario:  Adresseregister nede.
    Given   en melding med mottaker 998877445
    When    adresseregister svarer ikkje
    Then    skal vi får beskjed om at adresseregister er nede

  Scenario Outline: sende arkivspesifikk melding
    Given   melding med <meldingsformat>
    When    avsenders integrasjonspunkt skal sende melding
    Then    vi skal få svar om at melding har blitt formidlet
  Examples:
  |meldingsformat|
  |360           |
  |ePhorte       |
  |Akos          |



