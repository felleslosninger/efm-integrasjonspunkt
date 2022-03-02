package no.difi.meldingsutveksling.mail;

import com.sun.mail.smtp.SMTPTransport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.BestEduConverter;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.noarkexchange.PayloadUtil;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.DokumentType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.springframework.stereotype.Component;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
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

    private final Set<String> sessionSentIds = ConcurrentHashMap.newKeySet();

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

    public void send(PutMessageRequestType request, String title) {
        try {
            MimeMessage message = createMessage();
            message.setSubject(title, CHARSET);

            MimeMultipart mimeMultipart = new MimeMultipart();
            MimeBodyPart mimeBodyPart = new MimeBodyPart();

            if (PayloadUtil.isAppReceipt(request.getPayload())) {
                mimeBodyPart.setText("Kvittering (AppReceipt) mottatt.", CHARSET);
            } else {
                mimeBodyPart.setText("Du har fått en BestEdu melding. Se vedlegg for etadata og dokumenter.", CHARSET);

                MeldingType meldingType = BestEduConverter.payloadAsMeldingType(request.getPayload());
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
                String payload = BestEduConverter.meldingTypeAsString(meldingType);
                MimeBodyPart payloadAttachement = new MimeBodyPart();
                ByteArrayDataSource ds = new ByteArrayDataSource(payload.getBytes(), "application/xml;charset=" + CHARSET);
                payloadAttachement.setDataHandler(new DataHandler(ds));
                payloadAttachement.setFileName("payload.xml");
                mimeMultipart.addBodyPart(payloadAttachement);
            }

            mimeMultipart.addBodyPart(mimeBodyPart);
            message.setContent(mimeMultipart);

            Transport transport = message.getSession().getTransport("smtps");
            Long maxMessageSize = getMaxMessageSize(transport);
            if (maxMessageSize != null) {

                long messageSize = getMessageSize(message);
                if (messageSize > maxMessageSize) {

                    String conversationId = request.getEnvelope().getConversationId();
                    if (sessionSentIds.add(conversationId)) {
                        message.setText("Du har mottatt en BestEdu melding. Denne er for stor for å kunne sendes over e-post. Vennligst logg deg inn på FIKS portalen for å laste den ned.",
                                CHARSET);
                    } else {
                        log.info("Notification email was already sent for conversation ID = {}", conversationId);
                        return;
                    }
                }
            }

            Transport.send(message);
        } catch (MessagingException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

    private Long getMaxMessageSize(Transport transport) {
        return getMaxSizeFromMailServer(transport)
                .orElseGet(() -> props.getMail().getMaxSize());
    }

    private Optional<Long> getMaxSizeFromMailServer(Transport transport) {
        if (transport instanceof SMTPTransport) {
            SMTPTransport smtpTransport = (SMTPTransport) transport;
            return Optional.ofNullable(smtpTransport.getExtensionParameter("SIZE"))
                    .map(Long::valueOf);
        }

        return Optional.empty();
    }

    private long getMessageSize(MimeMessage m) {
        try (CountingOutputStream out = new CountingOutputStream(NULL_OUTPUT_STREAM)) {
            m.writeTo(out);
            return out.getByteCount() + 100L;
        } catch (IOException | MessagingException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }
}
