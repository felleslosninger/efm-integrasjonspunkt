Feature: Mottak av kvitterin på sendt medling fra sak/arkivløsning til /receive grensesnittet i integrasjonspunkt

  Scenario: Mottatt dokument på Receive er en kvittering
    Given et dokument mottatt på integrasjospunktet sitt receive grensesnitt er en kvittering
    When integrasjonspunktet mottar et dokument på receive rensesnit
    Then kvitteringen logges i integrasjonspunktet sin hendelseslogg
