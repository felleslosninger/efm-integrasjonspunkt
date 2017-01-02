package no.difi.meldingsutveksling.ks

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.ws.test.client.MockWebServiceServer

import javax.activation.DataHandler
import javax.mail.util.ByteArrayDataSource
import javax.xml.bind.JAXBContext
import javax.xml.bind.util.JAXBSource

import static org.springframework.ws.test.client.RequestMatchers.anything
import static org.springframework.ws.test.client.ResponseCreators.withPayload

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ContextConfiguration(classes = [SvarUtConfiguration, SvarUtWebServiceBeans])
class SvarUtWebServiceClientTest {
    @Autowired
    SvarUtWebServiceClientImpl client

    MockWebServiceServer server

    @Before
    void setup() {
        server = MockWebServiceServer.createServer(client)
    }

    @Test
    void "Testing to send message"() {
        byte[] data = new byte[1024]

        final Dokument dokument = Dokument.builder().withData(new DataHandler(new ByteArrayDataSource(data, "pdf"))).build()

        final Mottaker mottaker = Organisasjon.builder().build()


        final Printkonfigurasjon printkonfigurasjon = Printkonfigurasjon.builder().build()
        Forsendelse forsendelse = Forsendelse.builder()
                .withTittel("Tittel")
                .withAvgivendeSystem("Avgivende system?")
                .withDokumenter(dokument)
                .withMottaker(mottaker)
                .withPrintkonfigurasjon(printkonfigurasjon)
                .withKrevNiva4Innlogging(true)
                .withKryptert(true).build()

        SendForsendelseResponse payload = SendForsendelseResponse.builder().withReturn("123").build()
        JAXBContext context = JAXBContext.newInstance(payload.getClass())

        server.expect(anything()).andRespond(withPayload(new JAXBSource(context, payload)))
        client.sendMessage(forsendelse)

        server.verify()
    }

    @Test
    void "Testing to get status"() {
        RetrieveForsendelseStatus request = RetrieveForsendelseStatus.builder().withForsendelsesid("123").build()

        RetrieveForsendelseStatusResponse response = RetrieveForsendelseStatusResponse.builder().withReturn(ForsendelseStatus.MOTTATT).build()
        JAXBContext context = JAXBContext.newInstance(response.getClass())
        server.expect(anything()).andRespond(withPayload(new JAXBSource(context, response)))

        client.getForsendelseStatus("123")

        server.verify()
    }
}
