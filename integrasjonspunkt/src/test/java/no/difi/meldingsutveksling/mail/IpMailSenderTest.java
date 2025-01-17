package no.difi.meldingsutveksling.mail;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.xml.bind.JAXBException;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IpMailSenderTest {

    @Mock private IntegrasjonspunktProperties props;

    @InjectMocks private IpMailSender ipMailSender;

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
    }

    @Test
    public void testSend() {
        ipMailSender.send("foo", "foobody");
        assertThat(simpleSmtpServer.getReceivedEmails()).hasSize(1);

        SmtpMessage smtpMessage = simpleSmtpServer.getReceivedEmails().get(0);

        assertThat(smtpMessage.getHeaderValue("From")).isEqualTo("doofenshmirtz@evil.inc");
        assertThat(smtpMessage.getHeaderValue("To")).isEqualTo("stuntman@difi.no");
        assertThat(smtpMessage.getHeaderValue("Subject")).isEqualTo("foo");

        assertThat(smtpMessage.getBody()).isEqualTo("foobody");
    }

}
