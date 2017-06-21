package no.difi.meldingsutveksling.nextmove;

import com.google.common.collect.Lists;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.dpi.Document;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerRequest;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.PostAddress;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.apache.commons.io.FileUtils;
import org.springframework.http.MediaType;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class NextMoveDpiRequest implements MeldingsformidlerRequest {

    private static final String defaultMimeType = MediaType.APPLICATION_PDF_VALUE;

    private IntegrasjonspunktProperties props;
    private DpiConversationResource cr;
    private ServiceRecord serviceRecord;

    public NextMoveDpiRequest(IntegrasjonspunktProperties props, DpiConversationResource cr, ServiceRecord serviceRecord) {
        this.props = props;
        this.cr = cr;
        this.serviceRecord = serviceRecord;
    }

    @Override
    public Document getDocument() {
        String primaryFileName = cr.getFileRefs().get(0);
        return new Document(getContent(primaryFileName), getExtension(primaryFileName).orElse(defaultMimeType), primaryFileName, "Under utvikling");
    }

    @Override
    public List<Document> getAttachments() {
        final List<Document> docList = Lists.newArrayList();
        cr.getFileRefs().forEach((k, f) -> {
            if (k != 0) {
                docList.add(new Document(getContent(f), getExtension(f).orElse(defaultMimeType), f, "Under utvikling"));
            }
        });

        return docList;
    }

    private Optional<String> getExtension(String fileName) {
        return Stream.of(fileName.split(".")).reduce((a, b) -> b);
    }

    private byte[] getContent(String fileName) {
        String filedir = props.getNextbest().getFiledir();
        if (!filedir.endsWith("/")) {
            filedir = filedir + "/";
        }
        filedir = filedir + cr.getConversationId() + "/";
        File file = new File(filedir + fileName);

        byte[] content;
        try {
            content = FileUtils.readFileToByteArray(file);
        } catch (IOException e) {
            throw new NextMoveRuntimeException(String.format("Could not read file \"%s\"", fileName), e);
        }

        return content;
    }


    @Override
    public String getMottakerPid() {
        return cr.getReceiverId();
    }

    @Override
    public String getSubject() {
        return cr.getTitle();
    }

    @Override
    public String getSenderOrgnumber() {
        return cr.getSenderId();
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
