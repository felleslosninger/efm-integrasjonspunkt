package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerRequest;
import no.difi.meldingsutveksling.nextmove.v2.CryptoMessageResource;
import no.difi.meldingsutveksling.pipes.Reject;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.digdir.dpi.client.domain.Document;
import no.digdir.dpi.client.domain.MetadataDocument;
import no.digdir.dpi.client.domain.Parcel;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.vavr.Predicates.not;

@RequiredArgsConstructor
public class MeldingsformidlerRequestFactory {

    private final IntegrasjonspunktProperties properties;
    private final Clock clock;
    private final OptionalCryptoMessagePersister optionalCryptoMessagePersister;

    public MeldingsformidlerRequest getMeldingsformidlerRequest(NextMoveMessage nextMoveMessage, ServiceRecord serviceRecord, Reject reject) {
        MeldingsformidlerRequest.Builder builder = MeldingsformidlerRequest.builder()
                .standardBusinessDocumentHeader(nextMoveMessage.getSbd().getStandardBusinessDocumentHeader())
                .parcel(getParcel(nextMoveMessage, reject))
                .mottakerPid(nextMoveMessage.getReceiverIdentifier())
                .senderOrgnumber(nextMoveMessage.getSenderIdentifier())
                .onBehalfOfOrgnr(SBDUtil.getOnBehalfOfOrgNr(nextMoveMessage.getSbd()).orElse(null))
                .conversationId(nextMoveMessage.getConversationId())
                .postkasseAdresse(serviceRecord.getPostkasseAdresse())
                .certificate(serviceRecord.getPemCertificate().getBytes(StandardCharsets.UTF_8))
                .orgnrPostkasse(serviceRecord.getOrgnrPostkasse())
                .emailAddress(serviceRecord.getEpostAdresse())
                .mobileNumber(serviceRecord.getMobilnummer())
                .notifiable(serviceRecord.isKanVarsles())
                .virkningsdato(OffsetDateTime.now(clock))
                .language(properties.getDpi().getLanguage())
                .printColor(PrintColor.SORT_HVIT)
                .postalCategory(PostalCategory.B_OEKONOMI)
                .returnHandling(ReturnHandling.DIREKTE_RETUR);

        nextMoveMessage.getBusinessMessage(DpiMessage.class).ifPresent(dpiMessage ->
                builder.standardBusinessDocumentHeader(nextMoveMessage.getSbd().getStandardBusinessDocumentHeader())
                        .avsenderIdentifikator(dpiMessage.getAvsenderId())
                        .fakturaReferanse(dpiMessage.getFakturaReferanse())
        );

        nextMoveMessage.getBusinessMessage(DpiDigitalMessage.class).ifPresent(digital ->
                builder.subject(digital.getTittel())
                        .smsVarslingstekst(digital.getVarsler().getSmsTekst())
                        .emailVarslingstekst(digital.getVarsler().getEpostTekst())
                        .securityLevel(digital.getSikkerhetsnivaa())
                        .virkningsdato(digital.getDigitalPostInfo().getVirkningsdato().atStartOfDay(clock.getZone()).toOffsetDateTime())
                        .language(digital.getSpraak())
                        .aapningskvittering(digital.getDigitalPostInfo().getAapningskvittering())
        );

        nextMoveMessage.getBusinessMessage(DpiPrintMessage.class).ifPresent(print ->
                builder.postAddress(print.getMottaker())
                        .returnAddress(print.getRetur().getMottaker())
                        .printProvider(true)
                        .printColor(print.getUtskriftsfarge())
                        .postalCategory(print.getPosttype())
                        .returnHandling(print.getRetur().getReturhaandtering())
        );

        return builder.build();
    }

    private Parcel getParcel(NextMoveMessage nextMoveMessage, Reject reject) {
        return new Parcel()
                .setMainDocument(getMainDocument(nextMoveMessage, reject))
                .setAttachments(getAttachments(nextMoveMessage, reject));
    }

    private List<Document> getAttachments(NextMoveMessage nextMoveMessage, Reject reject) {
        return nextMoveMessage.getFiles()
                .stream()
                .filter(p -> p.getPrimaryDocument() == Boolean.FALSE)
                .filter(not(isMetadataFile()))
                .map(p -> getDocument(p, reject))
                .collect(Collectors.toList());
    }

    @NotNull
    private Predicate<BusinessMessageFile> isMetadataFile() {
        return p -> p.getMessage().getBusinessMessage(DpiMessage.class)
                .map(digital -> digital.getMetadataFiler().containsValue(p.getFilename()))
                .orElse(false);
    }

    private Document getMainDocument(NextMoveMessage nextMoveMessage, Reject reject) {
        return nextMoveMessage.getFiles().stream()
                .filter(p -> p.getPrimaryDocument() == Boolean.TRUE)
                .map(p -> getDocument(p, reject))
                .findFirst()
                .orElseThrow(() -> new NextMoveRuntimeException("No primary documents found, aborting send"));
    }

    private Document getDocument(BusinessMessageFile file, Reject reject) {
        return new Document()
                .setResource(getContent(file, reject))
                .setMimeType(file.getMimetype())
                .setFilename(file.getFilename())
                .setTitle(StringUtils.hasText(file.getTitle()) ? file.getTitle() : "Missing title")
                .setMetadataDocument(getMetadataDocument(file, reject));
    }

    private MetadataDocument getMetadataDocument(BusinessMessageFile file, Reject reject) {
        return file.getMessage().getBusinessMessage(DpiMessage.class)
                .filter(digital -> digital.getMetadataFiler().containsKey(file.getFilename()))
                .map(digital -> getMetadataDocument(file, reject, digital))
                .orElse(null);
    }

    private MetadataDocument getMetadataDocument(BusinessMessageFile file, Reject reject, DpiMessage digital) {
        String metadataFilename = digital.getMetadataFiler().get(file.getFilename());
        BusinessMessageFile messageFile = file.getMessage().getFiles()
                .stream()
                .filter(p -> p.getFilename().equals(metadataFilename))
                .findFirst()
                .orElseThrow(() -> new NextMoveRuntimeException("Metadata document $metadataFilename specified for ${file.filename}, but is not attached"));

        return new MetadataDocument()
                .setFilename(metadataFilename)
                .setMimeType(messageFile.getMimetype())
                .setResource(getContent(messageFile, reject));
    }

    private Resource getContent(BusinessMessageFile file, Reject reject) {
        return new CryptoMessageResource(file.getMessage().getMessageId(), file.getFilename(), optionalCryptoMessagePersister, reject);
    }
}
