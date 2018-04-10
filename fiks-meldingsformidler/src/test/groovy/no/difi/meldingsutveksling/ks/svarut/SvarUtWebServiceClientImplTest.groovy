package no.difi.meldingsutveksling.ks.svarut

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.oxm.jaxb.Jaxb2Marshaller
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.ws.test.client.MockWebServiceServer
import org.springframework.ws.test.client.RequestMatchers
import org.springframework.xml.transform.StringResult
import org.springframework.xml.transform.StringSource

import javax.activation.DataHandler
import javax.mail.util.ByteArrayDataSource
import javax.xml.bind.JAXBElement

import static org.springframework.ws.test.client.RequestMatchers.anything
import static org.springframework.ws.test.client.ResponseCreators.withPayload

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ContextConfiguration(classes = [SvarUtWebServiceBeans])
class SvarUtWebServiceClientImplTest {
    @Autowired
    SvarUtWebServiceClientImpl client

    @Autowired
    Jaxb2Marshaller marshaller

    MockWebServiceServer server

    @Before
    void setup() {
        server = MockWebServiceServer.createServer(client)
    }

    @Test
    void "Testing to send message"() {
        byte[] data = new byte[1024]

        final Dokument dokument = Dokument.builder().withData(new DataHandler(new ByteArrayDataSource(data, "pdf"))).build()

        final Adresse mottaker = Adresse.builder().build();

        final Printkonfigurasjon printkonfigurasjon = Printkonfigurasjon.builder().build()
        Forsendelse forsendelse = Forsendelse.builder()
                .withTittel("Tittel")
                .withAvgivendeSystem("Avgivende system?")
                .withDokumenter(dokument)
                .withMottaker(mottaker)
                .withPrintkonfigurasjon(printkonfigurasjon)
                .withKrevNiva4Innlogging(true)
                .withKryptert(true).build()

        SvarUtRequest request = new SvarUtRequest("http://localhost", forsendelse)

        SendForsendelseResponse response = SendForsendelseResponse.builder().withReturn("123").build()

        server.expect(anything()).andRespond(responseMatches(response))
        client.sendMessage(request)

        server.verify()
    }

    @Test
    void "Testing to get status"() {
        RetrieveForsendelseStatus request = RetrieveForsendelseStatus.builder().withForsendelsesid("123").build()

        RetrieveForsendelseStatusResponse response = RetrieveForsendelseStatusResponse.builder().withReturn(ForsendelseStatus.MOTTATT).build()

        server.expect(requestMatches(request)).andRespond(responseMatches(response))

        client.getForsendelseStatus("http://localhost", "123")

        server.verify()
    }

    @Test
    void "Getting forsendelseId returns a single forsendelseId"() {

        def conversationId = "be1a95d0-dd17-4fc5-a697-bedcc68f8b21"
        def forsendelseId = "edee9995-0110-43dd-a9a6-691f89c3a0f7"

        ObjectFactory objectFactory = new ObjectFactory()

        JAXBElement<RetrieveForsendelseIdByEksternRef> request = objectFactory.createRetrieveForsendelseIdByEksternRef(RetrieveForsendelseIdByEksternRef.builder().withEksternRef(conversationId).build())
        List<String> strings = new ArrayList<String>()
        strings.add(forsendelseId)
        JAXBElement<RetrieveForsendelseIdByEksternRefResponse> response = objectFactory.createRetrieveForsendelseIdByEksternRefResponse(RetrieveForsendelseIdByEksternRefResponse.builder().withReturn(strings).build())

        server.expect(requestMatches(request)).andRespond(responseMatches(response))

        client.getForsendelseId("http://localhost", conversationId)

        server.verify()
    }

    def requestMatches(jaxbObject) {
        return RequestMatchers.payload(marshalToStringSource(jaxbObject))
    }

    def responseMatches(jaxbObject) {
        return withPayload(marshalToStringSource(jaxbObject))
    }

    def marshalToStringSource(jaxbObject) {
        def result = new StringResult()
        marshaller.marshal(jaxbObject, result)
        return new StringSource(result.toString())
    }

}
