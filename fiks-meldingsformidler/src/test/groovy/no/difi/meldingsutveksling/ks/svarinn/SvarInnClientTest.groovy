package no.difi.meldingsutveksling.ks.svarinn

import no.difi.meldingsutveksling.ks.MockConfiguration
import no.difi.meldingsutveksling.receipt.ConversationService
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.RestTemplate

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

@RunWith(SpringJUnit4ClassRunner)
@SpringBootTest
@ContextConfiguration(classes = [MockConfiguration])
@ActiveProfiles("dev")
public class SvarInnClientTest {

    @Autowired
    RestTemplate restTemplate

    @Autowired
    SvarInnClient client
    private server

    @MockBean
    ConversationService conversationService

    @Before
    public void setup() {
        server = MockRestServiceServer.bindTo(restTemplate).build()
    }

    @Test
    public void checkForNewMessages() {
        this.server.expect(ExpectedCount.once(), requestTo(Matchers.endsWith("svarinn/mottaker/hentNyeForsendelser"))).andRespond(withSuccess(getClass().getResource("/sampleresponse.json").text, MediaType.APPLICATION_JSON))
        client.checkForNewMessages();

        this.server.verify()
    }

    @Test
    public void lastNedFil() {
        def forsendelse = new Forsendelse(downloadUrl: "path/to/file")
        def bytes = getClass().getResource("/decrypted-dokumenter-ae68b33d.zip").getBytes()

        this.server.expect(ExpectedCount.once(), requestTo(Matchers.equalTo(forsendelse.downloadUrl))).andRespond(withSuccess(new ByteArrayResource(bytes), MediaType.valueOf("application/zip;charset=UTF-8")))

        def receive = client.downloadZipFile(forsendelse)

        assert receive instanceof InputStream
        assert receive.bytes == bytes

        this.server.verify()
    }

    @Test
    public void testConfirmMessage() {
        def forsendelse = new Forsendelse(id: "123456")
        this.server.expect(ExpectedCount.once(),
                requestTo(Matchers.endsWith("svarinn/kvitterMottak/forsendelse/${forsendelse.id}"))).
                andRespond(withSuccess())

        client.confirmMessage(forsendelse.id)

        this.server.verify()
    }

}