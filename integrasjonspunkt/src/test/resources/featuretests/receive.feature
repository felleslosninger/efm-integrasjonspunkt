Feature: Mottak av kvittering sendt fra sak/arkivløsning. Kvittering er ikke en melding og skal bare logges.

  Scenario: Vi sender en kvittering til integrasjonspunktet
    Given en kvitering
    When integrasjonspunktet mottar en kvittering på putMessage grensesnittet
    Then kvitteringen logges i integrasjonspunktet sin hendelseslogg
    And kvitteringen sendes ikke videre til transport

