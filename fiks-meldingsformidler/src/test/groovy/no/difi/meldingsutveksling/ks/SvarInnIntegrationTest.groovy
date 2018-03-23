package no.difi.meldingsutveksling.ks

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.difi.meldingsutveksling.ks.svarinn.SvarInnBeans
import no.difi.meldingsutveksling.ks.svarinn.SvarInnClient
import no.difi.meldingsutveksling.ks.svarinn.SvarInnService
import no.difi.meldingsutveksling.ks.svarut.SvarUtConfiguration
import no.difi.meldingsutveksling.ks.svarut.SvarUtWebServiceBeans
import no.difi.meldingsutveksling.noarkexchange.NoarkClient
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType
import no.difi.meldingsutveksling.receipt.ConversationService
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.RestTemplate

import static org.mockito.Mockito.*
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

@RunWith(SpringJUnit4ClassRunner)
@SpringBootTest
@ContextConfiguration(classes = [SvarInnBeans, SvarUtWebServiceBeans, SvarUtConfiguration, MockConfiguration])
@EnableConfigurationProperties(IntegrasjonspunktProperties)
public class SvarInnIntegrationTest {

    @Autowired
    SvarInnService svarInnService

    @Autowired
    RestTemplate restTemplate

    private MockRestServiceServer server

    final String forsendelseId = "9b45ab9e-e13a-489e-832d-56e836c9a8bc"

    @Autowired
    IntegrasjonspunktProperties integrasjonspunktProperties

    @Autowired
    @Qualifier("localNoark")
    NoarkClient noarkClient

    @MockBean
    ConversationService conversationService;

    @Before
    public void setup() {
        server = MockRestServiceServer.bindTo(restTemplate).build()
        String response = getClass().getResource("/sampleresponse.json").text
        server.expect(requestTo(Matchers.endsWith("/svarinn/mottaker/hentNyeForsendelser"))).andRespond(withSuccess(response, MediaType.APPLICATION_JSON))
        when(noarkClient.sendEduMelding(Mockito.any(PutMessageRequestType.class))).thenReturn(new PutMessageResponseType(result: new AppReceiptType(type: "OK")))
    }

    @Test
    public void receiveMessageFromFiks() {
        byte[] zipFile = getClass().getResource("/somdalen-dokumenter-ae68b33d.zip").getBytes()
        server.expect(requestTo(Matchers.containsString("/svarinn/forsendelse/"))).andRespond(withSuccess(zipFile, SvarInnClient.APPLICATION_ZIP))
        server.expect(requestTo(Matchers.endsWith("svarinn/kvitterMottak/forsendelse/${forsendelseId}"))).andRespond(withSuccess())

        svarInnService.downloadFiles()

        verify(noarkClient).sendEduMelding(Mockito.any(PutMessageRequestType));
        server.verify()
    }

    @Test
    public void downloadedEmptyZipFile() {
        byte[] emptyZipFile = getClass().getResource("/empty_encrypted_svarinnfiles.zip").getBytes()
        server.expect(requestTo(Matchers.containsString("/svarinn/forsendelse/"))).andRespond(withSuccess(emptyZipFile, SvarInnClient.APPLICATION_ZIP))

        svarInnService.downloadFiles()
        verify(noarkClient, never()).sendEduMelding(Mockito.any(PutMessageRequestType))
    }
}
