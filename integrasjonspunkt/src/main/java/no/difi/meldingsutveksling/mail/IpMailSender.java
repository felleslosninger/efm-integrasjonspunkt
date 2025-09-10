package no.difi.meldingsutveksling.mail;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static com.google.common.base.Strings.isNullOrEmpty;

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

        Session session = Session.getInstance(mailProps,
                new jakarta.mail.Authenticator() {
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
