package no.difi.meldingsutveksling.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.springframework.stereotype.Component;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

import static com.google.common.base.Strings.isNullOrEmpty;

@RequiredArgsConstructor
@Slf4j
@Component
public class MailSender {

    private final IntegrasjonspunktProperties props;

    public void send(String title, String body) {
        Properties mailProps = new Properties();
        mailProps.put("mail.smtp.host", props.getMail().getSmtpHost());
        mailProps.put("mail.smtp.port", props.getMail().getSmtpPort());
        mailProps.put("mail.smtp.starttls.enable", "true");
        mailProps.put("mail.smtp.auth", props.getMail().getEnableAuth());

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

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(props.getMail().getSenderAddress()));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(props.getMail().getReceiverAddress()));

            message.setSubject(title);
            message.setText(body);

            Transport.send(message);
        } catch (MessagingException e) {
            log.error("Could not send mail", e);
        }
    }
}
