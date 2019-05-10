package no.difi.meldingsutveksling.ks

import no.difi.meldingsutveksling.UUIDGenerator
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.difi.meldingsutveksling.ks.mapping.FiksMapper
import no.difi.meldingsutveksling.ks.svarinn.SvarInnBeans
import no.difi.meldingsutveksling.ks.svarinn.SvarInnService
import no.difi.meldingsutveksling.ks.svarut.SvarUtConfiguration
import no.difi.meldingsutveksling.ks.svarut.SvarUtWebServiceBeans
import no.difi.meldingsutveksling.nextmove.message.CryptoMessagePersister
import no.difi.meldingsutveksling.noarkexchange.NoarkClient
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType
import no.difi.meldingsutveksling.receipt.MessageStatusFactory
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
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.RestTemplate

import static org.mockito.Mockito.*
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

@RunWith(SpringRunner)
@SpringBootTest
@ContextConfiguration(classes = [
        SvarInnBeans.class,
        SvarUtWebServiceBeans.class,
        SvarUtConfiguration.class,
        MockConfiguration,
        FiksMapper.class,
        MessageStatusFactory.class
])
@EnableConfigurationProperties(IntegrasjonspunktProperties)
class SvarInnIntegrationTest {

    @MockBean
    CryptoMessagePersister cryptoMessagePersister
    @MockBean
    UUIDGenerator uuidGenerator

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

    @Before
    void setup() {
        server = MockRestServiceServer.bindTo(restTemplate).build()
        String response = getClass().getResource("/sampleresponse.json").text
        server.expect(requestTo(Matchers.endsWith("/svarinn/mottaker/hentNyeForsendelser"))).andRespond(withSuccess(response, MediaType.APPLICATION_JSON))
        when(noarkClient.sendEduMelding(Mockito.any(PutMessageRequestType.class))).thenReturn(new PutMessageResponseType(result: new AppReceiptType(type: "OK")))
    }

    @Test
    void receiveMessageFromFiks() {
        byte[] zipFile = getClass().getResource("/somdalen-dokumenter-ae68b33d.zip").getBytes()
        server.expect(requestTo(Matchers.containsString("/svarinn/forsendelse/"))).andRespond(withSuccess(zipFile, MediaType.valueOf("application/zip;charset=UTF-8")))

        def forsendelser = svarInnService.getForsendelser()
        def attachments = svarInnService.getAttachments(forsendelser.get(0))

        assert attachments.count() == 1
        server.verify()
    }

    @Test
    void downloadedEmptyZipFile() {
        byte[] emptyZipFile = getClass().getResource("/empty_encrypted_svarinnfiles.zip").getBytes()
        server.expect(requestTo(Matchers.containsString("/svarinn/forsendelse/"))).andRespond(withSuccess(emptyZipFile, MediaType.valueOf("application/zip;charset=UTF-8")))

        def forsendelser = svarInnService.getForsendelser()
        def attachments = svarInnService.getAttachments(forsendelser.get(0))

        attachments.forEach { it -> it.inputStream.bytes }
        verify(noarkClient, never()).sendEduMelding(Mockito.any(PutMessageRequestType))
    }
}
