package no.difi.meldingsutveksling.ks.svarinn

import no.difi.meldingsutveksling.ks.svarinn.Forsendelse as Forsend
import org.hamcrest.Matchers
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
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
public class SvarInnClientTest {
    @Autowired
    RestTemplate restTemplate

    @Autowired
    SvarInnClient client

    @Test
    public void checkForNewMessages() {
        def server = MockRestServiceServer.bindTo(restTemplate).build()

        server.expect(ExpectedCount.once(), requestTo(Matchers.endsWith("svarinn/mottaker/hentNyeForsendelser"))).andRespond(withSuccess(new File("src/test/resources/sampleresponse.json").text, MediaType.APPLICATION_JSON))
        final List<Forsend> forsendelses = client.checkForNewMessages();
        println "<<< Forsendelse: $forsendelses";

        server.verify()
    }

}