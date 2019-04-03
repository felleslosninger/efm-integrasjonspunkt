package no.difi.meldingsutveksling.nextmove;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.dpi.Document;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerRequest;
import no.difi.meldingsutveksling.nextmove.message.CryptoMessagePersister;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;

@RequiredArgsConstructor
public class NextMoveDpiRequest implements MeldingsformidlerRequest {

    private static final String DEFAULT_EXT = "PDF";
    private static final String MISSING_TXT = "Missing title";

    private final IntegrasjonspunktProperties props;
    private final NextMoveMessage message;
    private final ServiceRecord serviceRecord;
    private final CryptoMessagePersister cryptoMessagePersister;

    @Override
    public Document getDocument() {
        BusinessMessageFile primary = message.getFiles().stream()
                .filter(BusinessMessageFile::getPrimaryDocument)
                .findFirst()
                .orElseThrow(() -> new NextMoveRuntimeException("No primary documents found, aborting send"));

        String title = isNullOrEmpty(primary.getTitle()) ? MISSING_TXT : primary.getTitle();
        return new Document(getContent(primary.getIdentifier()), primary.getMimetype(), primary.getFilename(), title);
    }

    @Override
    public List<Document> getAttachments() {
        final List<Document> docList = Lists.newArrayList();

        message.getFiles().stream().filter(f -> !f.getPrimaryDocument()).forEach(f -> {
            String title = isNullOrEmpty(f.getTitle()) ? MISSING_TXT : f.getTitle();
            docList.add(new Document(getContent(f.getIdentifier()), f.getMimetype(), f.getFilename(), title));
        });

        return docList;
    }

    private byte[] getContent(String fileName) {
        try {
            return cryptoMessagePersister.read(message.getConversationId(), fileName);
        } catch (IOException e) {
            throw new NextMoveRuntimeException(String.format("Could not read file \"%s\"", fileName), e);
        }
    }

    private boolean isDigitalMessage() {
        return this.message.getBusinessMessage() instanceof DpiDigitalMessage;
    }

    private boolean isPrintMessage() {
        return this.message.getBusinessMessage() instanceof DpiPrintMessage;
    }

    private DpiDigitalMessage getDigitalMessage() {
        return (DpiDigitalMessage) message.getBusinessMessage();
    }

    private DpiPrintMessage getPrintMessage() {
        return (DpiPrintMessage) message.getBusinessMessage();
    }

    @Override
    public String getMottakerPid() {
        return message.getReceiverIdentifier();
    }

    @Override
    public String getSubject() {
        if (message.getBusinessMessage() instanceof DpiDigitalMessage) {
            return((DpiDigitalMessage) message.getBusinessMessage()).getNonSensitiveTitle();
        }
        if (message.getBusinessMessage() instanceof DpiPrintMessage) {
            return null;
        }
        throw new NextMoveRuntimeException(String.format("BusinessMessage not instance of either %s or %s", DpiDigitalMessage.class.getName(), DpiPrintMessage.class.getName()));
    }

    @Override
    public String getSenderOrgnumber() {
        return message.getSenderIdentifier();
    }

    @Override
    public String getConversationId() {
        return message.getConversationId();
    }

    @Override
    public String getPostkasseAdresse() {
        return serviceRecord.getPostkasseAdresse();
    }

    @Override
    public byte[] getCertificate() {
        try {
            return serviceRecord.getPemCertificate().getBytes("UTF-8"); /* fra KRR via SR */
        } catch (UnsupportedEncodingException e) {
            throw new MeldingsUtvekslingRuntimeException("Pem certificate from servicerecord problems", e);
        }
    }

    @Override
    public String getOrgnrPostkasse() {
        return serviceRecord.getOrgnrPostkasse();
    }

    @Override
    public String getEmailAddress() {
        return serviceRecord.getEpostAdresse();
    }

    @Override
    public String getSmsVarslingstekst() {
        if (isDigitalMessage()) {
            return getDigitalMessage().getNotification().getSmsText();
        }
        return null;
    }

    @Override
    public String getEmailVarslingstekst() {
        if (isDigitalMessage()) {
            return getDigitalMessage().getNotification().getEmailText();
        }
        return null;
    }

    @Override
    public String getMobileNumber() {
        return serviceRecord.getMobilnummer();
    }

    @Override
    public boolean isNotifiable() {
        return serviceRecord.isKanVarsles();
    }

    @Override
    public boolean isPrintProvider() {
        return isPrintMessage();
    }

    @Override
    public PostAddress getPostAddress() {
        if (isPrintMessage()) {
            return getPrintMessage().getReceiver();
        }
        return null;
    }

    @Override
    public PostAddress getReturnAddress() {
        if (isPrintMessage()) {
            return getPrintMessage().getMailReturn().getReceiver();
        }
        return null;
    }
}
