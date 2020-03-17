package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import no.difi.begrep.sdp.schema_v10.SDPSikkerhetsnivaa;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dpi.Document;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerRequest;
import no.difi.meldingsutveksling.nextmove.message.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.sdp.client2.domain.MetadataDokument;
import no.difi.sdp.client2.domain.digital_post.Sikkerhetsnivaa;
import no.difi.sdp.client2.domain.fysisk_post.Posttype;
import no.difi.sdp.client2.domain.fysisk_post.Returhaandtering;
import no.difi.sdp.client2.domain.fysisk_post.Utskriftsfarge;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class NextMoveDpiRequest implements MeldingsformidlerRequest {

    private static final String MISSING_TXT = "Missing title";

    private final IntegrasjonspunktProperties props;
    private final Clock clock;
    private final NextMoveMessage message;
    private final ServiceRecord serviceRecord;
    private final OptionalCryptoMessagePersister optionalCryptoMessagePersister;

    @Override
    public Document getDocument() {
        BusinessMessageFile primary = message.getFiles().stream()
                .filter(BusinessMessageFile::getPrimaryDocument)
                .findFirst()
                .orElseThrow(() -> new NextMoveRuntimeException("No primary documents found, aborting send"));

        String title = StringUtils.hasText(primary.getTitle()) ? primary.getTitle() : MISSING_TXT;
        return new Document(getContent(primary.getIdentifier()), primary.getMimetype(), primary.getFilename(), title);
    }

    @Override
    public List<Document> getAttachments() {
        return message.getFiles()
                .stream()
                .filter(f -> !f.getPrimaryDocument())
                .filter(f -> !isMetadataFile(f.getFilename()))
                .map(this::createDocument)
                .collect(Collectors.toList());
    }

    private Document createDocument(BusinessMessageFile file) {
        String title = StringUtils.hasText(file.getTitle()) ? file.getTitle() : MISSING_TXT;
        Document document = new Document(getContent(file.getIdentifier()), file.getMimetype(), file.getFilename(), title);
        if (isDigitalMessage() && getDigitalMessage().getMetadataFiler().containsKey(file.getFilename())) {
            String metadataFilename = getDigitalMessage().getMetadataFiler().get(file.getFilename());
            BusinessMessageFile metadataFile = message.getFiles().stream()
                    .filter(f -> f.getFilename().equals(metadataFilename))
                    .findFirst()
                    .orElseThrow(() -> new NextMoveRuntimeException(
                            String.format("Metadata document \"%s\" specified for \"%s\", but is not attached", metadataFilename, file.getFilename())));
            MetadataDokument md = new MetadataDokument(metadataFilename, metadataFile.getMimetype(), getContent(metadataFile.getIdentifier()));
            document.setMetadataDokument(md);
        }
        return document;
    }

    private byte[] getContent(String fileName) {
        try {
            return optionalCryptoMessagePersister.read(message.getMessageId(), fileName);
        } catch (IOException e) {
            throw new NextMoveRuntimeException(String.format("Could not read file \"%s\"", fileName), e);
        }
    }

    private boolean isMetadataFile(String filename) {
        if (isPrintMessage()) {
            return false;
        }
        return getDigitalMessage().getMetadataFiler().containsValue(filename);
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
            return ((DpiDigitalMessage) message.getBusinessMessage()).getTittel();
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
    public Optional<String> getOnBehalfOfOrgnr() {
        return message.getSbd().getOnBehalfOfOrgNr();
    }

    @Override
    public Optional<String> getAvsenderIdentifikator() {
        if (isDigitalMessage()) {
            return Optional.ofNullable(getDigitalMessage().getAvsenderId());
        }
        return Optional.empty();
    }

    @Override
    public String getConversationId() {
        return message.getMessageId();
    }

    @Override
    public String getPostkasseAdresse() {
        return serviceRecord.getPostkasseAdresse();
    }

    @Override
    public byte[] getCertificate() {
        return serviceRecord.getPemCertificate().getBytes(StandardCharsets.UTF_8); /* fra KRR via SR */
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
            return getDigitalMessage().getVarsler().getSmsTekst();
        }
        return props.getDpi().getEmail().getVarslingstekst();
    }

    @Override
    public String getEmailVarslingstekst() {
        if (isDigitalMessage()) {
            return getDigitalMessage().getVarsler().getEpostTekst();
        }
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
        return isPrintMessage();
    }

    @Override
    public PostAddress getPostAddress() {
        if (isPrintMessage()) {
            return getPrintMessage().getMottaker();
        }
        return null;
    }

    @Override
    public PostAddress getReturnAddress() {
        if (isPrintMessage()) {
            return getPrintMessage().getRetur().getMottaker();
        }
        return null;
    }

    @Override
    public Sikkerhetsnivaa getSecurityLevel() {
        if (isDigitalMessage()) {
            SDPSikkerhetsnivaa sdpSikkerhetsnivaa = SDPSikkerhetsnivaa.fromValue(String.valueOf(message.getBusinessMessage().getSikkerhetsnivaa()));
            return Sikkerhetsnivaa.valueOf(sdpSikkerhetsnivaa.toString());
        }
        return null;
    }

    @Override
    public Date getVirkningsdato() {
        if (isDigitalMessage()) {
            return Date.from(getDigitalMessage().getDigitalPostInfo()
                    .getVirkningsdato()
                    .atStartOfDay()
                    .atZone(clock.getZone())
                    .toInstant());
        }
        return new Date();
    }

    @Override
    public String getLanguage() {
        if (isDigitalMessage()) {
            return getDigitalMessage().getSpraak();
        }
        return props.getDpi().getLanguage();
    }

    @Override
    public boolean isAapningskvittering() {
        if (isDigitalMessage()) {
            return getDigitalMessage().getDigitalPostInfo().getAapningskvittering();
        }
        return false;
    }

    @Override
    public Utskriftsfarge getPrintColor() {
        if (isPrintMessage()) {
            return getPrintMessage().getUtskriftsfarge();
        }
        return Utskriftsfarge.SORT_HVIT;
    }

    @Override
    public Posttype getPosttype() {
        if (isPrintMessage()) {
            return getPrintMessage().getPosttype();
        }
        return Posttype.B_OEKONOMI;
    }

    @Override
    public Returhaandtering getReturnHandling() {
        if (isPrintMessage()) {
            return getPrintMessage().getRetur().getReturhaandtering();
        }
        return Returhaandtering.DIREKTE_RETUR;
    }
}
