package no.difi.meldingsutveksling.mail;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IpMailSenderTest {

    @Mock private IntegrasjonspunktProperties props;

    @InjectMocks private IpMailSender ipMailSender;

    private static GreenMail simpleSmtpServer;

    @BeforeAll
    public static void beforeClass() {
        simpleSmtpServer = new GreenMail(ServerSetupTest.SMTP);
    }

    @AfterAll
    public static void afterClass() {
        simpleSmtpServer.stop();
    }

    @BeforeEach
    public void init() {
        simpleSmtpServer.reset();

        IntegrasjonspunktProperties.Mail mail = new IntegrasjonspunktProperties.Mail();
        mail.setSmtpHost("localhost");
        mail.setSenderAddress("doofenshmirtz@evil.inc");
        mail.setReceiverAddress("stuntman@difi.no");
        mail.setSmtpPort(String.valueOf(simpleSmtpServer.getSmtp().getPort()));
        mail.setEnableAuth("false");
        mail.setUsername("");
        mail.setPassword("");

        when(props.getMail()).thenReturn(mail);

        ipMailSender = new IpMailSender(props);
    }

    @Test
    public void testSend() throws MessagingException, IOException {
        ipMailSender.send("foo", "foobody");
        assertThat(simpleSmtpServer.getReceivedMessages()).hasSize(1);

        MimeMessage smtpMessage = simpleSmtpServer.getReceivedMessages()[0];

        assertThat(smtpMessage.getHeader("From")[0]).isEqualTo("doofenshmirtz@evil.inc");
        assertThat(smtpMessage.getHeader("To")[0]).isEqualTo("stuntman@difi.no");
        assertThat(smtpMessage.getHeader("Subject")[0]).isEqualTo("foo");

        assertThat(smtpMessage.getContent()).isEqualTo("foobody");
    }

}
