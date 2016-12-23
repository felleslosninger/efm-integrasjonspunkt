package no.difi.meldingsutveksling.ks.mapping

import no.difi.meldingsutveksling.core.EDUCore
import no.difi.meldingsutveksling.ks.mapping.edu.MeldingTypeHandler
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType
import spock.lang.Specification

class EDUCoreHandlerTest extends Specification {
    def "EDUCoreFosendelseHandler should contain needed Handlers to send message"() {
        given:
        EDUCore eduCore = new EDUCore(payload: new MeldingType())
        EDUCoreHandler handler = new EDUCoreHandler(eduCore)
        expect:
        handler.handlers.contains(new MeldingTypeHandler(eduCore.payloadAsMeldingType))
    }
}
