package no.difi.meldingsutveksling.ks.svarut

import no.difi.meldingsutveksling.ServiceIdentifier
import no.difi.meldingsutveksling.UUIDGenerator
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.difi.meldingsutveksling.core.EDUCore
import no.difi.meldingsutveksling.core.EDUCoreConverter
import no.difi.meldingsutveksling.core.Receiver
import no.difi.meldingsutveksling.core.Sender
import no.difi.meldingsutveksling.ks.mapping.FiksMapper
import no.difi.meldingsutveksling.ks.svarinn.SvarInnBeans
import no.difi.meldingsutveksling.ks.MockConfiguration
import no.difi.meldingsutveksling.nextmove.message.CryptoMessagePersister
import no.difi.meldingsutveksling.noarkexchange.schema.core.*
import no.difi.meldingsutveksling.receipt.MessageStatusFactory
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup
import no.difi.meldingsutveksling.serviceregistry.externalmodel.Service
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.ws.test.client.MockWebServiceServer
import javax.xml.bind.JAXBContext
import javax.xml.bind.util.JAXBSource

import static org.springframework.ws.test.client.RequestMatchers.anything
import static org.springframework.ws.test.client.ResponseCreators.withPayload

@RunWith(SpringRunner)
@SpringBootTest
@ContextConfiguration(classes = [
        SvarInnBeans.class,
        SvarUtWebServiceBeans.class,
        SvarUtConfiguration.class,
        MockConfiguration.class,
        FiksMapper.class,
        MessageStatusFactory.class
])
@EnableConfigurationProperties(IntegrasjonspunktProperties)
class SvarUtServiceIntegrationTest {

    @MockBean
    CryptoMessagePersister cryptoMessagePersister
    @MockBean
    UUIDGenerator uuidGenerator

    @Autowired
    SvarUtService service
    @Autowired
    SvarUtWebServiceClientImpl client
    @Autowired
    ServiceRegistryLookup serviceRegistryLookup

    MockWebServiceServer server

    @Before
    void setup() {
        server = MockWebServiceServer.createServer(client)

        SendForsendelseMedIdResponse payload = SendForsendelseMedIdResponse.builder().withReturn("19d5938b-31f1-4fdc-9d8c-cc30b9fadb3d").build()
        JAXBContext context = JAXBContext.newInstance(payload.getClass())
        server.expect(anything()).andRespond(withPayload(new JAXBSource(context, payload)))

        ServiceRecord serviceRecord = Mockito.mock(ServiceRecord.class)
        Service service = Mockito.mock(Service.class)
        Mockito.when(serviceRecord.getService()).thenReturn(service)
        Mockito.when(service.securityLevel).thenReturn(4)
        Mockito.when(serviceRegistryLookup.getServiceRecord(Mockito.any())).thenReturn(serviceRecord)
    }

    @Test
    void "test send"() {
        def eduCore = "an edu message"()


        def forsendelseId = service.send(eduCore)
        assert forsendelseId == "19d5938b-31f1-4fdc-9d8c-cc30b9fadb3d"

        server.verify()
    }

    def "an edu message"() {
        EDUCore eduCore = new EDUCore()
        def meldingType = new MeldingType(
                journpost: new JournpostType(
                        dokument: [new DokumentType(fil: new FilType(base64: [0x0, 0x1, 0x2]))],
                        jpJaar: "123",
                        jpSeknr: "123",
                        jpJpostnr: "123",
                        jpNdoktype: "foo",
                        jpStatus: "foo",
                        jpDokdato: "2017-01-01",
                        jpJdato: "2017-01-01"
                ),
                noarksak: new NoarksakType(
                        saSeknr: "123",
                        saOfftittel: "Test brev",
                        saSaar: "123")
        )
        eduCore.payload = EDUCoreConverter.meldingTypeAsString(meldingType)
        eduCore.sender = new Sender(name: "Difi", identifier: "991825827", ref: "123")
        eduCore.receiver = new Receiver(name: "Lote", identifier: "1234", ref: "123")
        eduCore.serviceIdentifier = ServiceIdentifier.DPF
        return eduCore
    }


}
