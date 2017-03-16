package no.difi.meldingsutveksling.ks

import no.difi.meldingsutveksling.CertificateParser
import no.difi.meldingsutveksling.core.EDUCore
import no.difi.meldingsutveksling.core.Receiver
import no.difi.meldingsutveksling.ks.mapping.ForsendelseMapper
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord
import spock.lang.Specification

class SvarUtServiceTest extends Specification {
    public static final String IDENTIFIER = "1234"
    private EDUCore domainMessage
    private SvarUtService service

    def "setup"() {

        def serviceRegistry = Mock(ServiceRegistryLookup)
        service = new SvarUtService(Mock(SvarUtWebServiceClient), serviceRegistry, Mock(ForsendelseMapper))
        service.certificateParser = Mock(CertificateParser)
        serviceRegistry.getServiceRecord(IDENTIFIER) >> new ServiceRecord(pemCertificate: "asdf")
    }

    def "When sending a domain message it is converted to forsendelse before being sent to svar ut"() {
        given:
        domainMessage = new EDUCore(receiver: new Receiver(identifier: IDENTIFIER))
        def forsendelse = new Forsendelse()
        SvarUtRequest request = new SvarUtRequest("", forsendelse)

        when:
        service.send(domainMessage)

        then:
        1 * service.forsendelseMapper.mapFrom(this.domainMessage, _) >> forsendelse
        then:
        1 * service.client.sendMessage(request)
    }
}
