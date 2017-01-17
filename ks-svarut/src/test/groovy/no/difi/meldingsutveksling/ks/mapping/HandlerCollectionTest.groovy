package no.difi.meldingsutveksling.ks.mapping

import no.difi.meldingsutveksling.ks.Forsendelse
import spock.lang.Specification

class HandlerCollectionTest extends Specification {
    def "HandlerCollection should map forsendelse on all contained handlers"() {
        given:
        HandlerCollection collection = new HandlerCollection()
        def simpleHandler = Mock(Handler)
        collection.handlers.add(simpleHandler)
        def forsendelse = Forsendelse.builder()
        when:
        collection.map(forsendelse)
        then:
        1*simpleHandler.map(forsendelse)
    }
}
