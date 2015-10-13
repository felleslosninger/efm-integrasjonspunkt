Feature: Vi ønsker at integrasjonspunktet skal kunne ta imot meldinger fra ulike arkivsystem og videreformidle disse

  Scenario Outline: Integrasjonspunkt mottar en melding som blir videresendt
    Given en velformet melding fra <arkivsystem>
    And virksomhet finnes i Adresseregisteret
    When integrasjonspunktet mottar meldingen
    Then skal melding bli videresendt
  Examples:
    |arkivsystem|
    |ePhorte    |
    |p360       |
    |akoz       |