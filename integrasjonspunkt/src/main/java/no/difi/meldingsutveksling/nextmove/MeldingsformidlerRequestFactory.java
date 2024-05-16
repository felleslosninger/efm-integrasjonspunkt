package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dokumentpakking.domain.Document;
import no.difi.meldingsutveksling.dokumentpakking.domain.MetadataDocument;
import no.difi.meldingsutveksling.dokumentpakking.domain.Parcel;
import no.difi.meldingsutveksling.domain.ICD;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.PersonIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerRequest;
import no.difi.meldingsutveksling.nextmove.v2.CryptoMessageResource;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.meldingsutveksling.status.MpcIdHolder;
import no.difi.move.common.io.pipe.Reject;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.vavr.Predicates.not;

@RequiredArgsConstructor
public class MeldingsformidlerRequestFactory {

    private final IntegrasjonspunktProperties properties;
    private final Clock clock;
    private final OptionalCryptoMessagePersister optionalCryptoMessagePersister;
    private final MpcIdHolder mpcIdHolder;

    public MeldingsformidlerRequest getMeldingsformidlerRequest(NextMoveMessage nextMoveMessage, Reject reject) {
        MeldingsformidlerRequest.Builder builder = MeldingsformidlerRequest.builder()
                .parcel(getParcel(nextMoveMessage))
                .sender(nextMoveMessage.getSender().cast(Iso6523.class).toMainOrganization())
                .onBehalfOf(SBDUtil.getPartIdentifier(nextMoveMessage.getSbd()).orElse(null))
                .messageId(nextMoveMessage.getMessageId())
                .conversationId(nextMoveMessage.getConversationId())
                .mpcId(mpcIdHolder.getNextMpcId())
                .expectedResponseDateTime(nextMoveMessage.getSbd().getExpectedResponseDateTime().orElse(null))
                .virkningsdato(OffsetDateTime.now(clock))
                .language(properties.getDpi().getLanguage())
                .printColor(PrintColor.SORT_HVIT)
                .postalCategory(PostalCategory.B_OEKONOMI)
                .returnHandling(ReturnHandling.DIREKTE_RETUR);

        if (nextMoveMessage.getReceiver() != null) {
            builder.mottakerPid(nextMoveMessage.getReceiver().cast(PersonIdentifier.class));
        }

        nextMoveMessage.getBusinessMessage(DpiMessage.class).ifPresent(dpiMessage ->
                builder.avsenderIdentifikator(dpiMessage.getAvsenderId())
                        .fakturaReferanse(dpiMessage.getFakturaReferanse())
        );

        nextMoveMessage.getBusinessMessage(DpiDigitalMessage.class).ifPresent(digital ->
                builder.subject(digital.getTittel())
                        .smsVarslingstekst(getSmsVarslingstekst(digital))
                        .emailVarslingstekst(getEmailVarslingstekst(digital))
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

    private Parcel getParcel(NextMoveMessage nextMoveMessage) {
        return Parcel.builder()
                .mainDocument(getMainDocument(nextMoveMessage))
                .attachments(getAttachments(nextMoveMessage))
                .build();
    }

    private String getEmailVarslingstekst(DpiDigitalMessage digital) {
        return Optional.ofNullable(digital.getVarsler())
                .map(DpiNotification::getEpostTekst)
                .orElse(null);
    }

    private String getSmsVarslingstekst(DpiDigitalMessage digital) {
        return Optional.ofNullable(digital.getVarsler())
                .map(DpiNotification::getSmsTekst)
                .orElse(null);
    }

    private String getMobilnummer(ServiceRecord serviceRecord) {
        return StringUtils.hasLength(serviceRecord.getMobilnummer()) ? serviceRecord.getMobilnummer() : null;
    }

    private List<Document> getAttachments(NextMoveMessage nextMoveMessage) {
        return nextMoveMessage.getFiles()
                .stream()
                .filter(p -> p.getPrimaryDocument() == Boolean.FALSE)
                .filter(not(isMetadataFile(nextMoveMessage)))
                .map(p -> getDocument(nextMoveMessage, p))
                .collect(Collectors.toList());
    }

    @NotNull
    private Predicate<BusinessMessageFile> isMetadataFile(NextMoveMessage nextMoveMessage) {
        return p -> nextMoveMessage.getBusinessMessage(DpiDigitalMessage.class)
                .map(digital -> digital.getMetadataFiler().containsValue(p.getFilename()))
                .orElse(false);
    }

    private Document getMainDocument(NextMoveMessage nextMoveMessage) {
        return nextMoveMessage.getFiles().stream()
                .filter(p -> p.getPrimaryDocument() == Boolean.TRUE)
                .map(p -> getDocument(nextMoveMessage, p))
                .findFirst()
                .orElseThrow(() -> new NextMoveRuntimeException("No primary documents found, aborting send"));
    }

    private Document getDocument(NextMoveMessage nextMoveMessage, BusinessMessageFile file) {
        return Document.builder()
                .resource(getContent(nextMoveMessage, file))
                .mimeType(file.getMimetype())
                .filename(file.getFilename())
                .title(StringUtils.hasText(file.getTitle()) ? file.getTitle() : "Missing title")
                .metadataDocument(getMetadataDocument(nextMoveMessage, file))
                .build();
    }

    private MetadataDocument getMetadataDocument(NextMoveMessage nextMoveMessage, BusinessMessageFile file) {
        return nextMoveMessage.getBusinessMessage(DpiDigitalMessage.class)
                .filter(digital -> digital.getMetadataFiler().containsKey(file.getFilename()))
                .map(digital -> getMetadataDocument(nextMoveMessage, file, digital))
                .orElse(null);
    }

    private MetadataDocument getMetadataDocument(NextMoveMessage nextMoveMessage, BusinessMessageFile file, DpiDigitalMessage digital) {
        String metadataFilename = digital.getMetadataFiler().get(file.getFilename());
        if (nextMoveMessage.getFiles() == null) {
            return null;
        }
        BusinessMessageFile messageFile = nextMoveMessage.getFiles()
                .stream()
                .filter(p -> p.getFilename().equals(metadataFilename))
                .findFirst()
                .orElseThrow(() -> new NextMoveRuntimeException(String.format("Metadata document %s specified for %s, but is not attached", metadataFilename, file.getFilename())));

        return MetadataDocument.builder()
                .filename(metadataFilename)
                .mimeType(messageFile.getMimetype())
                .resource(getContent(nextMoveMessage, messageFile))
                .build();
    }

    private Resource getContent(NextMoveMessage nextMoveMessage, BusinessMessageFile file) {
        return new CryptoMessageResource(nextMoveMessage.getMessageId(), file.getIdentifier(), optionalCryptoMessagePersister);
    }
}
