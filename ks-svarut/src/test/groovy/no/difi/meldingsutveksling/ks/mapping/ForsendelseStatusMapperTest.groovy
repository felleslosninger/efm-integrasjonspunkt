package no.difi.meldingsutveksling.ks.mapping

import no.difi.meldingsutveksling.ks.ForsendelseStatus
import no.difi.meldingsutveksling.receipt.ReceiptStatus
import spock.lang.Specification
import spock.lang.Unroll

class ForsendelseStatusMapperTest extends Specification {

    @Unroll
    "given ForsendelseStatus.#forsendelsestatus should map to ReceiptStatus.#receiptStatus"() {
        given:
        def mapper = new ForsendelseStatusMapper()

        expect:
        mapper.mapFrom(forsendelsestatus) == receiptStatus
        where:
        forsendelsestatus                  | receiptStatus
        ForsendelseStatus.LEST             | ReceiptStatus.READ
        ForsendelseStatus.MOTTATT          | ReceiptStatus.DELIVERED
        ForsendelseStatus.AVVIST           | ReceiptStatus.FAIL
        ForsendelseStatus.AKSEPTERT        | ReceiptStatus.SENT
        ForsendelseStatus.IKKE_LEVERT      | ReceiptStatus.OTHER
        ForsendelseStatus.MANUELT_HANDTERT | ReceiptStatus.DELIVERED
        ForsendelseStatus.LEVERT_SDP       | ReceiptStatus.DELIVERED
        ForsendelseStatus.PRINTET          | ReceiptStatus.DELIVERED
        ForsendelseStatus.SENDT_DIGITALT   | ReceiptStatus.DELIVERED
        ForsendelseStatus.SENDT_PRINT      | ReceiptStatus.DELIVERED
        ForsendelseStatus.SENDT_SDP        | ReceiptStatus.DELIVERED
        ForsendelseStatus.VARSLET          | ReceiptStatus.DELIVERED
    }
}
