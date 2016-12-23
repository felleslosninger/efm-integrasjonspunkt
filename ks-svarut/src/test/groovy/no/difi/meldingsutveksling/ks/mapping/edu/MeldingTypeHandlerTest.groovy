package no.difi.meldingsutveksling.ks.mapping.edu

import no.difi.meldingsutveksling.ks.Forsendelse
import no.difi.meldingsutveksling.noarkexchange.schema.core.DokumentType
import no.difi.meldingsutveksling.noarkexchange.schema.core.FilType
import no.difi.meldingsutveksling.noarkexchange.schema.core.JournpostType
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType
import no.difi.meldingsutveksling.noarkexchange.schema.core.NoarksakType
import spock.lang.Specification

class MeldingTypeHandlerTest extends Specification {

    def "Mapping a MeldingType with two documents to Forsendelse"() {
        given:
        def meldingType = "a meldingstype with two documents"()

        MeldingTypeHandler handler = new MeldingTypeHandler(meldingType)
        when:
        def builderResult = handler.map(Forsendelse.builder()).build()
        then:
        assert builderResult.dokumenter.size() == meldingType.journpost.dokument.size()
        assert builderResult.tittel == meldingType.noarksak.saOfftittel
    }

    def "a meldingstype with two documents"() {
        def type = new MeldingType()
        type.noarksak = new NoarksakType(saOfftittel: "tittel")
        type.journpost = new JournpostType()
        def dok1 = new DokumentType(dbTittel: "dokument1.pdf", fil: new FilType(base64: [1, 2, 3]), veFilnavn: "dokument1.pdf", veMimeType: "pdf")
        def dok2 = new DokumentType(dbTittel: "dokument2.pdf", fil: new FilType(base64: [2, 3, 4]), veFilnavn: "dokument1.pdf", veMimeType: "pdf")
        type.journpost.dokument << dok1 << dok2
        return type
    }
}
