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
    private DpiConversationResource cr;
    private ServiceRecord serviceRecord;
    private MessagePersister messagePersister;

    public NextMoveDpiRequest(IntegrasjonspunktProperties props,
                              MessagePersister messagePersister,
                              DpiConversationResource cr,
                              ServiceRecord serviceRecord) {
        this.props = props;
        this.messagePersister = messagePersister;
        this.cr = cr;
        this.serviceRecord = serviceRecord;
    }

    @Override
    public Document getDocument() {
        String primaryFileName = cr.getFileRefs().get(0);
        String title;
        if (cr.getCustomProperties() != null) {
            title = cr.getCustomProperties().getOrDefault(primaryFileName, MISSING_TXT);
        } else {
            title = MISSING_TXT;
        }
        return new Document(getContent(primaryFileName), getMime(getExtension(primaryFileName)), primaryFileName, title);
    }

    @Override
    public List<Document> getAttachments() {
        final List<Document> docList = Lists.newArrayList();
        cr.getFileRefs().forEach((k, f) -> {
            if (k != 0) {
                String title;
                if (cr.getCustomProperties() != null) {
                    title = cr.getCustomProperties().getOrDefault(f, MISSING_TXT);
                } else {
                    title = MISSING_TXT;
                }
                docList.add(new Document(getContent(f), getMime(getExtension(f)), f, title));
            }
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
            return messagePersister.read(cr, fileName);
        } catch (IOException e) {
            throw new NextMoveRuntimeException(String.format("Could not read file \"%s\"", fileName), e);
        }
    }


    @Override
    public String getMottakerPid() {
        return cr.getReceiver().getReceiverId();
    }

    @Override
    public String getSubject() {
//        return cr.getTitle();
        // TODO: replace with hoveddokument title
        return "";
    }

    @Override
    public String getSenderOrgnumber() {
        return cr.getSender().getSenderId();
    }

    @Override
    public String getConversationId() {
        return cr.getConversationId();
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
        return props.getDpi().getSms().getVarslingstekst();
    }

    @Override
    public String getEmailVarslingstekst() {
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
        return serviceRecord.isFysiskPost() || (props.getDpi().isForcePrint() && isNullOrEmpty(serviceRecord.getPostkasseAdresse()));
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
