package no.difi.meldingsutveksling.mail;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.xml.transform.StringSource;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IpMailSenderTest {

    @Mock private IntegrasjonspunktProperties props;

    @InjectMocks private IpMailSender ipMailSender;

    private Unmarshaller unmarshaller;

    private static SimpleSmtpServer simpleSmtpServer;

    @BeforeAll
    public static void beforeClass() throws IOException {
        simpleSmtpServer = SimpleSmtpServer.start(SimpleSmtpServer.AUTO_SMTP_PORT);
    }

    @AfterAll
    public static void afterClass() {
        simpleSmtpServer.stop();
    }

    @BeforeEach
    public void init() throws JAXBException {
        simpleSmtpServer.reset();

        IntegrasjonspunktProperties.Mail mail = new IntegrasjonspunktProperties.Mail();
        mail.setSmtpHost("localhost");
        mail.setSenderAddress("doofenshmirtz@evil.inc");
        mail.setReceiverAddress("stuntman@difi.no");
        mail.setSmtpPort(String.valueOf(simpleSmtpServer.getPort()));
        mail.setEnableAuth("false");
        mail.setUsername("");
        mail.setPassword("");

        when(props.getMail()).thenReturn(mail);

        ipMailSender = new IpMailSender(props);

        JAXBContext putMessageJaxbContext = JAXBContext.newInstance(PutMessageRequestType.class);
        this.unmarshaller = putMessageJaxbContext.createUnmarshaller();
    }

    @Test
    public void testSend() throws JAXBException, IOException {
        send();
        assertThat(simpleSmtpServer.getReceivedEmails()).hasSize(1);

        SmtpMessage smtpMessage = simpleSmtpServer.getReceivedEmails().get(0);

        assertThat(smtpMessage.getHeaderValue("From")).isEqualTo("doofenshmirtz@evil.inc");
        assertThat(smtpMessage.getHeaderValue("To")).isEqualTo("stuntman@difi.no");
        assertThat(smtpMessage.getHeaderValue("Subject")).isEqualTo("foo");

        assertThat(smtpMessage.getBody()).hasLineCount(4);
    }

    @Test
    public void testSendTooLarge() throws JAXBException, IOException {
        props.getMail().setMaxSize(40000L);
        send();
        assertThat(simpleSmtpServer.getReceivedEmails()).hasSize(1);

        SmtpMessage smtpMessage = simpleSmtpServer.getReceivedEmails().get(0);

        assertThat(smtpMessage.getHeaderValue("From")).isEqualTo("doofenshmirtz@evil.inc");
        assertThat(smtpMessage.getHeaderValue("To")).isEqualTo("stuntman@difi.no");
        assertThat(smtpMessage.getHeaderValue("Subject")).isEqualTo("foo");

        assertThat(smtpMessage.getBody()).isEqualTo("Du har mottatt en BestEdu melding. Denne er for stor for =E5 kunne sendes o=ver e-post. Vennligst logg deg inn p=E5 FIKS portalen for =E5 laste den ned=..");
    }

    @Test
    public void testSendTooLargeTwice() throws JAXBException, IOException {
        props.getMail().setMaxSize(40000L);
        send();
        send();
        assertThat(simpleSmtpServer.getReceivedEmails()).hasSize(1);

        SmtpMessage smtpMessage = simpleSmtpServer.getReceivedEmails().get(0);

        assertThat(smtpMessage.getHeaderValue("From")).isEqualTo("doofenshmirtz@evil.inc");
        assertThat(smtpMessage.getHeaderValue("To")).isEqualTo("stuntman@difi.no");
        assertThat(smtpMessage.getHeaderValue("Subject")).isEqualTo("foo");

        assertThat(smtpMessage.getBody()).isEqualTo("Du har mottatt en BestEdu melding. Denne er for stor for =E5 kunne sendes o=ver e-post. Vennligst logg deg inn p=E5 FIKS portalen for =E5 laste den ned=..");
    }

    private void send() throws JAXBException, IOException {
        PutMessageRequestType putMessage = createPutMessageCdataXml(FileUtils.readFileToString(new File
                ("src/test/resources/putmessage_test.xml"), StandardCharsets.UTF_8));
        ipMailSender.send(putMessage, "foo");
    }

    private PutMessageRequestType createPutMessageCdataXml(String payload) throws JAXBException {
        return unmarshaller.unmarshal(new StringSource((payload)), PutMessageRequestType.class).getValue();
    }
}
