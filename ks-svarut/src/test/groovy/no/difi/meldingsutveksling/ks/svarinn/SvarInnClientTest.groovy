package no.difi.meldingsutveksling.ks.svarinn

import no.difi.meldingsutveksling.ks.svarinn.Forsendelse as Forsend
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
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
@ContextConfiguration(classes = [SvarInnBeans])
@ActiveProfiles("dev")
public class SvarInnClientTest {
    @Autowired
    RestTemplate restTemplate

    @Autowired
    SvarInnClient client
    private server

    @Before
    public void setup() {
        server = MockRestServiceServer.bindTo(restTemplate).build()
    }

    @Test
    public void checkForNewMessages() {
        this.server.expect(ExpectedCount.once(), requestTo(Matchers.endsWith("svarinn/mottaker/hentNyeForsendelser"))).andRespond(withSuccess(new File("src/test/resources/sampleresponse.json").text, MediaType.APPLICATION_JSON))
        final List<Forsend> forsendelses = client.checkForNewMessages();
        println "<<< Forsendelse: $forsendelses";

        this.server.verify()
    }

    @Test
    public void lastNedFil() {
        String downloadUrl = "path/to/file"
        Resource resource = new ByteArrayResource(new File("src/test/resources/Testdokument.pdf").getBytes())


        this.server.expect(ExpectedCount.once(), requestTo(Matchers.equalTo(downloadUrl))).andRespond(withSuccess(resource, SvarInnClient.APPLICATION_ZIP))

        def receive = client.downloadFile(downloadUrl)

        this.server.verify()

        assert receive instanceof SvarInnFile
        assert receive.mediaType == SvarInnClient.APPLICATION_ZIP
        assert receive.contents == new File("src/test/resources/Testdokument.pdf").getBytes()
    }

}