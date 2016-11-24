package no.difi.meldingsutveksling.mail;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.util.Properties;

public class EduMailSender {

    private IntegrasjonspunktProperties properties;

    public EduMailSender(IntegrasjonspunktProperties properties) {
        this.properties = properties;
    }

    public void send(byte[] messageAsBytes, String title) {
        Properties props = new Properties();
        props.put("mail.smtp.host", properties.getMail().getSmtpHost());
        props.put("mail.smtp.port", properties.getMail().getSmtpPort());
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.auth", "true");

        Session session = Session.getDefaultInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(properties.getMail().getUsername(),
                                properties.getMail().getPassword());
                    }
                }
        );

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("integrasjonspunkt@difi.no"));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(properties.getMail().getReceiverAddress()));
            message.setSubject("Melding fra integrasjonspunkt: "+title);

            MimeMultipart mimeMultipart = new MimeMultipart();
            // Add content
            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setText("Melding vedlagt.");
            mimeMultipart.addBodyPart(mimeBodyPart);
            // Add attachement
            ByteArrayDataSource ds = new ByteArrayDataSource(messageAsBytes, "application/octet-stream");
            MimeBodyPart attachementPart = new MimeBodyPart();
            attachementPart.setDataHandler(new DataHandler(ds));
            attachementPart.setFileName(title+".xml");
            mimeMultipart.addBodyPart(attachementPart);

            message.setContent(mimeMultipart);

            Transport.send(message);
        } catch (MessagingException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }
}
