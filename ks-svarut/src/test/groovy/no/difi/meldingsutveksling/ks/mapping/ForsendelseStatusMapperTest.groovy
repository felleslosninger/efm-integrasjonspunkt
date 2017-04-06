package no.difi.meldingsutveksling.ks.mapping

import no.difi.meldingsutveksling.ks.ForsendelseStatus
import no.difi.meldingsutveksling.receipt.GenericReceiptStatus
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
        ForsendelseStatus.LEST             | GenericReceiptStatus.READ
        ForsendelseStatus.MOTTATT          | GenericReceiptStatus.DELIVERED
        ForsendelseStatus.AVVIST           | GenericReceiptStatus.FAIL
        ForsendelseStatus.AKSEPTERT        | GenericReceiptStatus.SENT
        ForsendelseStatus.IKKE_LEVERT      | GenericReceiptStatus.OTHER
        ForsendelseStatus.MANUELT_HANDTERT | GenericReceiptStatus.DELIVERED
        ForsendelseStatus.LEVERT_SDP       | GenericReceiptStatus.DELIVERED
        ForsendelseStatus.PRINTET          | GenericReceiptStatus.DELIVERED
        ForsendelseStatus.SENDT_DIGITALT   | GenericReceiptStatus.DELIVERED
        ForsendelseStatus.SENDT_PRINT      | GenericReceiptStatus.DELIVERED
        ForsendelseStatus.SENDT_SDP        | GenericReceiptStatus.DELIVERED
        ForsendelseStatus.VARSLET          | GenericReceiptStatus.DELIVERED
    }
}
