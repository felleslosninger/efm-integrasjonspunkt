package no.difi.meldingsutveksling.ks

import no.difi.meldingsutveksling.core.EDUCore
import no.difi.meldingsutveksling.ks.mapping.Handler
import no.difi.meldingsutveksling.ks.mapping.HandlerFactory
import spock.lang.Specification

class EDUCoreConverterImplTest extends Specification {
    EDUCoreConverterImpl eduConverter

    def setup() {
        eduConverter = new EDUCoreConverterImpl(Mock(HandlerFactory))
    }

    def "EduConverter should use all converters provided by handlerFactory when converting eduCore"() {
        given:
        EDUCore eduCore = new EDUCore()
        def handlers = new ArrayList<Handler<Forsendelse.Builder>>()
        def handler = Mock(Handler)
        handlers.addAll(handler, handler)

        when:
        this.eduConverter.convert(eduCore)
        then:
        eduConverter.handlerFactory.createHandlers(eduCore) >> handlers

        2*handler.map(_ as Forsendelse.Builder)
    }
}