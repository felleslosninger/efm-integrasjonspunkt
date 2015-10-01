Feature: klargjøre melding for sikker sending

  Scenario: sender og mottakers virksomhetssertifikat er gyldig
    Given   mottaker med orgnummer 910094092 har gyldig sertifikat
    And     avsender med orgnummer 910094092 har gyldig sertifikat
    When    vi sender melding
    Then    vi skal fa svar om at melding har blitt formidlet

  Scenario: sender og mottakers virksomhetssertifikat er ugyldig
    Given   mottaker med orgnummer 998877450 har ugyldig sertifikat
    And     avsender med orgnummer 998877310 har ugyldig sertifikat
    When    vi sender melding relatert til ugyldige sertifikater
    Then    vi skal få svar om at melding ikkje kan sendes

  Scenario: avsender eller mottaker sitt virksomhetssertifikat er ugyldig
    Given   mottaker med orgnummer 998877442 som finnes i adresseregisteret
    And     avsender med orgnummer 998877310 som finnes i adresseresiteret
    When    vi sender melding
    Then    vi skal få svar om at melding ikkje kan sendes

  Scenario: sender og mottakers virksomhetssertifikat finnes ikke
    Given   mottaker med orgnummer 998877400 som finnes i adresseregisteret
    And     avsender med orgnummer 998877333 som finnes i adresseresiteret
    When    vi sender melding
    Then    vi skal få svar om at melding ikkje kan sendes

  Scenario: avsender eller mottakers virksomhetssertifikat finnes ikke
    Given   mottaker med orgnummer 998877444 som finnes i adresseregisteret
    And     avsender med orgnummer 998877300 som finnes i adresseresiteret
    When    vi sender melding
    Then    vi skal få svar om at melding ikkje kan sendes
#
  Scenario: Pakke og Signere meldinger
#kalle pakke metode med norak melding(må inneholde div attributter) --> pakke melding alt skal gå bra, signere så pakken
#pakke ikke ok, kan ikkje signere
# Legger på adresse info som orgnummer + BEST/EDU meldingen pakkes i en ASiC dokumentpakke som signeres og krypteres.
# Kan sjekke om orgnummer finnes
    Given 
    And 
    When 
    Then