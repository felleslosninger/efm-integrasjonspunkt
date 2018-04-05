package no.difi.meldingsutveksling.nextmove;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.config.dpi.InkType;
import no.difi.meldingsutveksling.config.dpi.PrintSettings;
import no.difi.meldingsutveksling.config.dpi.ReturnType;
import no.difi.meldingsutveksling.config.dpi.ShippingType;
import no.difi.meldingsutveksling.config.dpi.securitylevel.SecurityLevel;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.dpi.Document;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerRequest;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.PostAddress;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Strings.isNullOrEmpty;
import static no.difi.meldingsutveksling.MimeTypeExtensionMapper.getMimetype;

@Slf4j
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
        Optional<FileAttachement> hdo = cr.getFiles().stream().filter(FileAttachement::isHoveddokument).findFirst();
        FileAttachement hd = hdo.orElseThrow(() -> new NextMoveRuntimeException("No 'hoveddokument' supplied in file attachments"));

        if (isNullOrEmpty(hd.getFilnavn())) {
            throw new NextMoveRuntimeException("'filnavn' missing for 'hoveddokument', aborting");
        }

        String title;
        if (isNullOrEmpty(hd.getTittel())) {
            log.warn("'Tittel' missing for 'hoveddokument', defaulting to {}", MISSING_TXT);
            title = MISSING_TXT;
        } else {
            title = hd.getTittel();
        }

        String mime;
        if (isNullOrEmpty(hd.getMimetype())) {
            log.warn("'mimetype' missing for 'hoveddokument', mapping by extension..");
            mime = getMime(getExtension(hd.getFilnavn()));
        } else {
            mime = hd.getMimetype();
        }

        return new Document(getContent(hd.getFilnavn()), mime, hd.getFilnavn(), title);
    }

    @Override
    public List<Document> getAttachments() {
        List<FileAttachement> files = cr.getFiles().stream().filter(f -> !f.isHoveddokument()).collect(Collectors.toList());
        final List<Document> docList = Lists.newArrayList();
        for (FileAttachement f : files) {
            if (!cr.getFileRefs().values().contains(f.getFilnavn())) {
                throw new NextMoveRuntimeException(String.format("Attachment %s has no supplied file", f.getFilnavn()));
            }

            String mime;
            if (isNullOrEmpty(f.getMimetype())) {
                log.warn("No mimetype set for file attachment {}, mapping mime based on extension", f);
                mime = getMime(getExtension(f.getFilnavn()));
            } else {
                mime = f.getMimetype();
            }

            String title;
            if (isNullOrEmpty(f.getTittel())) {
                log.warn("No 'tittel' set for file attachment {}, defaulting to {}", f, MISSING_TXT);
                title = MISSING_TXT;
            } else {
                title = f.getTittel();
            }
            docList.add(new Document(getContent(f.getFilnavn()), mime, f.getFilnavn(), title));
        }

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
        if (cr.getDigitalPostInfo() != null && !isNullOrEmpty(cr.getDigitalPostInfo().getIkkeSensitivTittel())) {
            // valider
            return cr.getDigitalPostInfo().getIkkeSensitivTittel();
        }

        Optional<FileAttachement> hdo = cr.getFiles().stream()
                .filter(FileAttachement::isHoveddokument)
                .findFirst();
        FileAttachement hd = hdo.orElseThrow(() -> new NextMoveRuntimeException("No 'hoveddokument' supplied in file attachments"));
        return hd.getTittel();
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
        return cr.getDigitalPostInfo().getVarsler().getSmsVarsel().getTekst();
    }

    @Override
    public String getEmailVarslingstekst() {
        return cr.getDigitalPostInfo().getVarsler().getEpostVarsel().getTekst();
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

    @Override
    public String getLanguage() {
        return cr.getSpraak();
    }

    @Override
    public PrintSettings getPrintSettings() {
        return PrintSettings.builder()
                .inkType(InkType.fromExternal(cr.getFysiskPostInfo().getUtskriftsfarge()))
                .returnType(ReturnType.fromExternal(cr.getFysiskPostInfo().getRetur().getPostHaandtering()))
                .shippingType(ShippingType.fromExternal(cr.getFysiskPostInfo().getPosttype()))
                .build();
    }

    @Override
    public SecurityLevel getSecurityLevel() {
        return cr.getSecurityLevel();
    }

    @Override
    public Date getVirkningsdato() {
        return Date.from(cr.getDigitalPostInfo().getVirkningsdato().atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    @Override
    public boolean getAapningskvittering() {
        return cr.getDigitalPostInfo().getAapningskvittering();
    }
}
