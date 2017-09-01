package no.difi.meldingsutveksling.mail;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCoreConverter;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.noarkexchange.PayloadUtil;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.DokumentType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.util.List;
import java.util.Properties;

import static com.google.common.base.Strings.isNullOrEmpty;

public class EduMailSender {

    private IntegrasjonspunktProperties properties;

    public EduMailSender(IntegrasjonspunktProperties properties) {
        this.properties = properties;
    }

    public void send(PutMessageRequestType request, String title) {
        Properties props = new Properties();
        props.put("mail.smtp.host", properties.getMail().getSmtpHost());
        props.put("mail.smtp.port", properties.getMail().getSmtpPort());
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.auth", properties.getMail().getEnableAuth());

        String trust = properties.getMail().getTrust();
        if (!isNullOrEmpty(trust)) {
            props.put("mail.smtp.ssl.trust", trust);
        }

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
            message.setFrom(new InternetAddress(properties.getMail().getSenderAddress()));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(properties.getMail().getReceiverAddress()));
            message.setSubject(title);

            MimeMultipart mimeMultipart = new MimeMultipart();
            MimeBodyPart mimeBodyPart = new MimeBodyPart();

            if (PayloadUtil.isAppReceipt(request.getPayload())) {
                mimeBodyPart.setText("Kvittering (AppReceipt) mottatt.");
            } else {
                mimeBodyPart.setText("Du har fått en BestEdu melding. Se vedlegg for metadata og dokumenter.");

                MeldingType meldingType = EDUCoreConverter.payloadAsMeldingType(request.getPayload());
                List<DokumentType> docs = meldingType.getJournpost().getDokument();
                for (DokumentType doc : docs) {
                    ByteArrayDataSource ds = new ByteArrayDataSource(doc.getFil().getBase64(), doc.getVeMimeType());
                    MimeBodyPart attachementPart = new MimeBodyPart();
                    attachementPart.setDataHandler(new DataHandler(ds));
                    attachementPart.setFileName(doc.getVeFilnavn());
                    mimeMultipart.addBodyPart(attachementPart);
                    // Set file content to null since we want the payload xml later as attachement
                    doc.getFil().setBase64(null);
                }
                String payload = EDUCoreConverter.meldingTypeAsString(meldingType);
                MimeBodyPart payloadAttachement = new MimeBodyPart();
                ByteArrayDataSource ds = new ByteArrayDataSource(payload.getBytes(), "application/xml");
                payloadAttachement.setDataHandler(new DataHandler(ds));
                payloadAttachement.setFileName("payload.xml");
                mimeMultipart.addBodyPart(payloadAttachement);
            }

            mimeMultipart.addBodyPart(mimeBodyPart);
            message.setContent(mimeMultipart);

            Transport.send(message);
        } catch (MessagingException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

}
