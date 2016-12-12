package no.difi.meldingsutveksling.ks

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.ws.test.client.MockWebServiceServer

import javax.xml.bind.JAXBContext
import javax.xml.bind.util.JAXBSource

import static org.springframework.ws.test.client.RequestMatchers.anything
import static org.springframework.ws.test.client.ResponseCreators.withPayload

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = [SvarUtConfiguration, SvarUtWebServiceTestConfiguration])
class SvarUtWebServiceClientTest {
    @Autowired
    SvarUtWebServiceClient client

    MockWebServiceServer server

    @Before
    void setup() {
        server = MockWebServiceServer.createServer(client)
    }

    @Test
    void "SendMessage"() {
        Forsendelse forsendelse = Forsendelse.builder().addDokumenter(Dokument.builder().build()).build()
        //server.expect(payload(forsendelse)).andRespond("1234")
        SendForsendelseResponse.builder().withReturn("123")

        SendForsendelseResponse payload = SendForsendelseResponse.builder().withReturn("123").build()
        ObjectFactory objectFactory = new ObjectFactory()
        JAXBContext context = JAXBContext.newInstance(payload.getClass())

        server.expect(anything()).andRespond(withPayload(new JAXBSource(context, objectFactory.createSendForsendelseResponse(payload))))
        client.sendMessage()
        server.verify()
    }
}
