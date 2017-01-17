package no.difi.meldingsutveksling.ks

import no.difi.meldingsutveksling.core.EDUCore
import no.difi.meldingsutveksling.core.Receiver
import no.difi.meldingsutveksling.core.Sender
import no.difi.meldingsutveksling.noarkexchange.schema.core.DokumentType
import no.difi.meldingsutveksling.noarkexchange.schema.core.FilType
import no.difi.meldingsutveksling.noarkexchange.schema.core.JournpostType
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType
import no.difi.meldingsutveksling.noarkexchange.schema.core.NoarksakType
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.ws.test.client.MockWebServiceServer

import javax.xml.bind.JAXBContext
import javax.xml.bind.util.JAXBSource

import static org.springframework.ws.test.client.RequestMatchers.anything
import static org.springframework.ws.test.client.ResponseCreators.withPayload

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ContextConfiguration(classes = [SvarUtConfiguration, SvarUtWebServiceBeans])
class SvarUtServiceIntegrationTest {
    @Autowired
    SvarUtService service

    @Autowired
    SvarUtWebServiceClientImpl client

    MockWebServiceServer server

    @Before
    void setup() {
        server = MockWebServiceServer.createServer(client)

        SendForsendelseResponse payload = SendForsendelseResponse.builder().withReturn("123").build()
        JAXBContext context = JAXBContext.newInstance(payload.getClass())
        server.expect(anything()).andRespond(withPayload(new JAXBSource(context, payload)))
    }

    @Test
    void "test send"() {
        def eduCore = "an edu message"()


        def forsendelseId = service.send(eduCore)
        assert forsendelseId == "123"

        server.verify()
    }

    def "an edu message"() {
        EDUCore eduCore = new EDUCore()
        eduCore.payload = new MeldingType(journpost:
                new JournpostType(
                        dokument: [new DokumentType(fil: new FilType(base64: [0x0,0x1,0x2]))]
                ),
                noarksak: new NoarksakType(saOfftittel: "Test brev")
        )
        eduCore.sender = new Sender(name: "Difi", identifier: "991825827")
        eduCore.receiver = new Receiver(name: "Lote", identifier: "1234")
        return eduCore
    }


}
