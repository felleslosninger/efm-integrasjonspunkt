package no.difi.meldingsutveksling.ks.mapping

import no.difi.meldingsutveksling.ks.ForsendelseStatus
import no.difi.meldingsutveksling.ks.receipt.DpfReceiptStatus
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
        ForsendelseStatus.LEST             | DpfReceiptStatus.LEST
        ForsendelseStatus.MOTTATT          | DpfReceiptStatus.MOTTATT
        ForsendelseStatus.AVVIST           | DpfReceiptStatus.AVVIST
        ForsendelseStatus.AKSEPTERT        | DpfReceiptStatus.AKSEPTERT
        ForsendelseStatus.IKKE_LEVERT      | DpfReceiptStatus.IKKE_LEVERT
        ForsendelseStatus.MANUELT_HANDTERT | DpfReceiptStatus.MANULT_HANDTERT
        ForsendelseStatus.LEVERT_SDP       | DpfReceiptStatus.LEVERT_SDP
        ForsendelseStatus.PRINTET          | DpfReceiptStatus.PRINTET
        ForsendelseStatus.SENDT_DIGITALT   | DpfReceiptStatus.SENDT_DIGITALT
        ForsendelseStatus.SENDT_PRINT      | DpfReceiptStatus.SENDT_PRINT
        ForsendelseStatus.SENDT_SDP        | DpfReceiptStatus.SENDT_SDP
        ForsendelseStatus.VARSLET          | DpfReceiptStatus.VARSLET
    }
}
