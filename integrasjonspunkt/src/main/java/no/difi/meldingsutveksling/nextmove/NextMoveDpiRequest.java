package no.difi.meldingsutveksling.nextmove;

import com.google.common.collect.Lists;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.dpi.Document;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerRequest;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.PostAddress;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.stream.Stream;

import static com.google.common.base.Strings.isNullOrEmpty;
import static no.difi.meldingsutveksling.MimeTypeExtensionMapper.getMimetype;

public class NextMoveDpiRequest implements MeldingsformidlerRequest {

    private static final String DEFAULT_EXT = "PDF";
    private static final String MISSING_TXT = "Missing title";

    private IntegrasjonspunktProperties props;
    private NextMoveMessage message;
    private ServiceRecord serviceRecord;
    private MessagePersister messagePersister;

    public NextMoveDpiRequest(IntegrasjonspunktProperties props,
                              MessagePersister messagePersister,
                              NextMoveMessage message,
                              ServiceRecord serviceRecord) {
        this.props = props;
        this.messagePersister = messagePersister;
        this.message = message;
        this.serviceRecord = serviceRecord;
    }

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

    private String getMime(String ext) {
        // DPI specific override
        if ("XML".equals(ext)) {
            return "application/ehf+xml";
        }
        return getMimetype(ext);
    }

    private String getExtension(String fileName) {
        return Stream.of(fileName.split(".")).reduce((a, b) -> b).orElse(DEFAULT_EXT);
    }

    private byte[] getContent(String fileName) {
        try {
            return messagePersister.read(message.getConversationId(), fileName);
        } catch (IOException e) {
            throw new NextMoveRuntimeException(String.format("Could not read file \"%s\"", fileName), e);
        }
    }


    @Override
    public String getMottakerPid() {
        return message.getReceiverIdentifier();
    }

    @Override
    public String getSubject() {
        if (!(message.getBusinessMessage() instanceof DpiMessage)) {
            throw new NextMoveRuntimeException("BusinessMessage not instance of DpiMessage");
        }
        DpiMessage dpiMessage = (DpiMessage) message.getBusinessMessage();
        return dpiMessage.getTitle();
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
        // TODO get from businessmessage
        return props.getDpi().getSms().getVarslingstekst();
    }

    @Override
    public String getEmailVarslingstekst() {
        // TODO get from businessmessage
        return props.getDpi().getEmail().getVarslingstekst();
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
        return serviceRecord.isFysiskPost();
    }

    @Override
    public PostAddress getPostAddress() {
        return serviceRecord.getPostAddress();
    }

    @Override
    public PostAddress getReturnAddress() {
        return serviceRecord.getReturnAddress();
    }
}
