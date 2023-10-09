package no.difi.meldingsutveksling.mail;

import com.sun.mail.smtp.SMTPTransport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import org.apache.commons.io.output.CountingOutputStream;
import org.springframework.stereotype.Component;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.apache.commons.io.output.NullOutputStream.NULL_OUTPUT_STREAM;

@Slf4j
@RequiredArgsConstructor
@Component
public class IpMailSender {

    private static final String CHARSET = StandardCharsets.ISO_8859_1.name();

    private final IntegrasjonspunktProperties props;

    private MimeMessage createMessage() throws MessagingException {
        Properties mailProps = new Properties();
        mailProps.put("mail.smtp.host", props.getMail().getSmtpHost());
        mailProps.put("mail.smtp.port", props.getMail().getSmtpPort());
        mailProps.put("mail.smtp.starttls.enable", "true");
        if (!isNullOrEmpty(props.getMail().getEnableAuth())) {
            mailProps.put("mail.smtp.auth", props.getMail().getEnableAuth());
        }

        String trust = props.getMail().getTrust();
        if (!isNullOrEmpty(trust)) {
            mailProps.put("mail.smtp.ssl.trust", trust);
        }

        Session session = Session.getDefaultInstance(mailProps,
                new javax.mail.Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(props.getMail().getUsername(),
                                props.getMail().getPassword());
                    }
                }
        );

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(props.getMail().getSenderAddress()));
        message.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse(props.getMail().getReceiverAddress()));

        return message;
    }

    public void send(String title, String body) {
        try {
            MimeMessage message = createMessage();

            message.setSubject(title, CHARSET);
            message.setText(body);

            Transport.send(message);
        } catch (MessagingException e) {
            log.error("Could not send mail", e);
        }
    }

}
