package no.difi.meldingsutveksling.ks.mapping

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.difi.meldingsutveksling.core.EDUCore
import no.difi.meldingsutveksling.ks.mapping.edu.ReceiverHandler
import spock.lang.Specification

class HandlerFactoryTest extends Specification {
    def "test createHandlers"() {
        given:
            def properties = new IntegrasjonspunktProperties()
            def handlerFactory =  new HandlerFactory(properties)
            def eduCore = new EDUCore()

        when:
        def handlers = handlerFactory.createHandlers(eduCore)
        then:
        handlers.find { it instanceof HandlerCollection }
        handlers.find { it instanceof PropertiesHandler}
        handlers.find { it instanceof ReceiverHandler}
    }
}
