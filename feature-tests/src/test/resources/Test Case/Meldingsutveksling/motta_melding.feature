Feature: Hente meldinger fra Altinn formidlingstjeneste og sende tilsak/arkivløsningen sin importmodul

  Scenario: Altinn har nye meldinger
    Given Altinn har melding til mottaker som mottaker finner
    When mottaker sjekker etter nye meldinger
    Then mottaker mottar liste over nye meldinger

  Scenario: Altinn har ikke nye meldinger
    Given Altinn har ikke melding til mottaker
    When mottaker sjekker etter nye meldinger
    Then mottaker mottar tom liste

  Scenario Outline: Mottar arkivspesifikk melding
    Given ny <meldingsformat> melding
    When mottaker mottar melding
    Then arkivsystem skal lagre melding
  Examples:
    |meldingsformat|
    |360           |
    |eforte        |
    |akos          |
