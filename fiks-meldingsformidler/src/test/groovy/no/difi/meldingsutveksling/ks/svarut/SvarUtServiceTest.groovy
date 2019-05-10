package no.difi.meldingsutveksling.ks.svarut

import no.difi.meldingsutveksling.CertificateParser
import no.difi.meldingsutveksling.config.FiksConfig
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.difi.meldingsutveksling.core.EDUCore
import no.difi.meldingsutveksling.core.Receiver
import no.difi.meldingsutveksling.ks.mapping.FiksMapper
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord
import spock.lang.Specification

import static no.difi.meldingsutveksling.ServiceIdentifier.DPF

class SvarUtServiceTest extends Specification {
    public static final String IDENTIFIER = "1234"
    private EDUCore domainMessage
    private SvarUtService service

    def "setup"() {

        def serviceRegistry = Mock(ServiceRegistryLookup)
        def props = Mock(IntegrasjonspunktProperties)
        service = new SvarUtService(Mock(SvarUtWebServiceClient), serviceRegistry, Mock(FiksMapper), props)
        service.setCertificateParser(Mock(CertificateParser))
        serviceRegistry.getServiceRecord(IDENTIFIER, DPF) >> new ServiceRecord(pemCertificate: "asdf")
        def fiksConfig = Mock(FiksConfig)
        def svarUtConfig = Mock(FiksConfig.SvarUt)
        svarUtConfig.endpointUrl >> new URL("http://foo")
        fiksConfig.getUt() >> svarUtConfig
        props.getFiks() >> fiksConfig
    }

    def "When sending a domain message it is converted to forsendelse before being sent to svar ut"() {
        given:
        domainMessage = new EDUCore(receiver: new Receiver(identifier: IDENTIFIER), serviceIdentifier: DPF)
        def forsendelse = new SendForsendelseMedId()
        SvarUtRequest request = new SvarUtRequest("http://foo", forsendelse)

        when:
        service.send(domainMessage)

        then:
        1 * service.fiksMapper.mapFrom(this.domainMessage, _) >> forsendelse
        then:
        1 * service.client.sendMessage(request)
    }
}
