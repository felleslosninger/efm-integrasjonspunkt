package no.difi.meldingsutveksling.ks

import no.difi.meldingsutveksling.core.EDUCore
import spock.lang.Specification

class SvarUtServiceTest extends Specification {
    private EDUCore domainMessage
    private SvarUtService service

    def "setup"() {

        service = new SvarUtService(Mock(EDUCoreConverter), Mock(SvarUtWebServiceClient))
    }

    def "When sending a domain message it is converted to forsendelse before being sent to svar ut"() {
        given:
        domainMessage = new EDUCore()
        Forsendelse forsendelse = new Forsendelse()

        when:
        service.send(domainMessage)

        then:
        1 * service.messageConverter.convert(this.domainMessage) >> forsendelse
        then:
        1 * service.client.sendMessage(forsendelse)
    }
}
