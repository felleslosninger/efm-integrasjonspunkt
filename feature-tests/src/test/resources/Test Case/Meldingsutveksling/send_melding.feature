Feature: Motta dokument fra SAK/ARKIV og videresende


  Scenario: Altinn nede
    Given SBD dokument skal videresendes til Altinn
    And Altinn er nede
    When vi sender dokumentet til Røn sitt integrasjonspunkt
    Then feilmelding returneres tilbake til SAK/ARKIV

  Scenario: JournalpostId hentes fra Best/EDU melding formatert ihht, Fylkesmannen i Sogn og Fjordane  sitt sak/arkiv-system
    Given BEST/EDU dokument fra Fylkesmannen i Sogn og Fjordane
    When vi sender dokumentet til Røn sitt integrasjonspunkt
    Then JournalpostId skal finnes i SBD dokumentet

  Scenario: JournalpostId hentes fra public 360 formatert best/EDU
    Given BEST/EDU dokument fra public 360
    When vi sender dokumentet til Røn sitt integrasjonspunkt
    Then JournalpostId skal finnes i SBD dokumentet

  Scenario: Avsender eller mottaker har ugyldige virksomhetssertifikater
    Given avsender med ugyldig virksomhetssertifikat
    And mottaker med ugyldig virksomhetssertifikat
    When vi sender dokumentet til Røn sitt integrasjonspunkt
    Then dokumentet blir ikke sendt
    Then feilmelding returneres tilbake til SAK/ARKIV

  Scenario: Sender melding
    Given en velformert melding med mottaker Stålheim
    And mottaker finnes i adresseregister
    When vi sender dokumentet til Røn sitt integrasjonspunkt
    #Then dokumentet blir videreformidlet til Stålheim # <-- lurer på om vi skal ha denne
    And melding om at dokument er sendt videre returneres til SAK/ARKIV

