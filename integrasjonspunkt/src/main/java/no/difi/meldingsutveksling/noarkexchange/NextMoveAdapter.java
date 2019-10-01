package no.difi.meldingsutveksling.noarkexchange;

import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.difi.meldingsutveksling.DocumentType;
import no.difi.meldingsutveksling.MimeTypeExtensionMapper;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.UUIDGenerator;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.BestEduConverter;
import no.difi.meldingsutveksling.dokumentpakking.service.SBDFactory;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.arkivmelding.ArkivmeldingFactory;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.ArkivmeldingKvitteringMessage;
import no.difi.meldingsutveksling.nextmove.ArkivmeldingMessage;
import no.difi.meldingsutveksling.nextmove.KvitteringStatusMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.nextmove.v2.BasicNextMoveFile;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageService;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static no.difi.meldingsutveksling.NextMoveConsts.ARKIVMELDING_FILE;

@Slf4j
@Component
@RequiredArgsConstructor
public class NextMoveAdapter {

    private final NextMoveMessageService nextMoveMessageService;
    private final ArkivmeldingFactory arkivmeldingFactory;
    private final SBDFactory createSBD;
    private final IntegrasjonspunktProperties properties;
    private final UUIDGenerator uuidGenerator;
    private final ServiceRegistryLookup srLookup;
    private final ConversationIdEntityRepo conversationIdEntityRepo;

    public PutMessageResponseType convertAndSend(PutMessageRequestWrapper message) {
        NextMoveOutMessage nextMoveMessage;
        if (PayloadUtil.isAppReceipt(message.getPayload())) {
            nextMoveMessage = convertAppReceipt(message);
        } else {
            try {
                nextMoveMessage = convertEduMessage(message);
            } catch (PayloadException e) {
                log.error("Error parsing payload", e);
                return PutMessageResponseFactory.createErrorResponse(e.getLocalizedMessage());
            } catch (JAXBException e) {
                log.error("Error marshalling arkivmelding converted from EDU document", e);
                return PutMessageResponseFactory.createErrorResponse("Error converting to arkivmelding");
            }
        }

        nextMoveMessageService.sendMessage(nextMoveMessage);

        return PutMessageResponseFactory.createOkResponse();
    }

    private NextMoveOutMessage convertAppReceipt(PutMessageRequestWrapper message) {
        AppReceiptType appReceiptType = BestEduConverter.payloadAsAppReceipt(message.getPayload());
        ArkivmeldingKvitteringMessage receipt = new ArkivmeldingKvitteringMessage(appReceiptType.getType(), message.getConversationId(), Sets.newHashSet());
        appReceiptType.getMessage().forEach(sm -> receipt.getMessages().add(new KvitteringStatusMessage(sm.getCode(), sm.getText())));
        StandardBusinessDocument sbd = createSBD.createNextMoveSBD(Organisasjonsnummer.from(message.getSenderPartynumber()),
                Organisasjonsnummer.from(message.getReceiverPartyNumber()),
                message.getConversationId(),
                uuidGenerator.generate(),
                properties.getArkivmelding().getReceiptProcess(),
                DocumentType.ARKIVMELDING_KVITTERING,
                receipt);
        return nextMoveMessageService.createMessage(sbd);
    }

    private NextMoveOutMessage convertEduMessage(PutMessageRequestWrapper message) throws PayloadException, JAXBException {
        String conversationId = message.getConversationId();
        try {
            UUID.fromString(conversationId);
        } catch (IllegalArgumentException e) {
            conversationId = uuidGenerator.generate();
            log.warn("PutMessage has conversationId={} which is not a valid UUID. Setting new conversationId: {}", message.getConversationId(), conversationId);
            conversationIdEntityRepo.save(new ConversationIdEntity(message.getConversationId(), conversationId));
        }
        ServiceRecord receiverServiceRecord;
        try {
            receiverServiceRecord = srLookup.getServiceRecord(SRParameter.builder(message.getReceiverPartyNumber()).build(),
                    properties.getArkivmelding().getDefaultProcess(),
                    DocumentType.ARKIVMELDING);
        } catch (ServiceRegistryLookupException e) {
            throw new MeldingsUtvekslingRuntimeException(String.format("Error looking up service record for %s", message.getReceiverPartyNumber()), e);
        }
        StandardBusinessDocument sbd = createSBD.createNextMoveSBD(Organisasjonsnummer.from(message.getSenderPartynumber()),
                Organisasjonsnummer.from(message.getReceiverPartyNumber()),
                conversationId,
                conversationId,
                properties.getArkivmelding().getDefaultProcess(),
                DocumentType.ARKIVMELDING,
                new ArkivmeldingMessage()
                        .setSikkerhetsnivaa(receiverServiceRecord.getService().getSecurityLevel())
                        .setHoveddokument(ARKIVMELDING_FILE)
        );

        return nextMoveMessageService.createMessage(sbd, getFiles(message));
    }

    private List<BasicNextMoveFile> getFiles(PutMessageRequestWrapper message) throws JAXBException, PayloadException {
        List<NoarkDocument> noarkDocuments = PayloadUtil.parsePayloadForDocuments(message.getPayload());

        // Need to check if receiver is on FIKS platform. If so, reject if documents are not PDF.
        ServiceRecord serviceRecord;
        try {
            serviceRecord = srLookup.getServiceRecord(SRParameter.builder(message.getReceiverPartyNumber()).build(),
                    properties.getArkivmelding().getDefaultProcess(),
                    DocumentType.ARKIVMELDING);
        } catch (ServiceRegistryLookupException e) {
            throw new MeldingsUtvekslingRuntimeException(String.format("Could not find service record for receiver %s", message.getReceiverPartyNumber()), e);
        }
        if (serviceRecord.getService().getIdentifier() == ServiceIdentifier.DPF) {
            if (noarkDocuments.stream()
                    .map(d -> MediaType.valueOf(d.getContentType()))
                    .anyMatch(mt -> !MediaType.APPLICATION_PDF.equals(mt))) {
                throw new MeldingsUtvekslingRuntimeException(String.format("Target service for %s is SvarUt, which only supports PDF documents.", message.getReceiverPartyNumber()));
            }
        }

        List<BasicNextMoveFile> files = noarkDocuments.stream()
                .map(d -> BasicNextMoveFile.of(d.getTitle(), d.getFilename(), d.getContentType(), Base64.getDecoder().decode(d.getContent())))
                .collect(Collectors.toList());
        files.add(getArkivmeldingFile(message));
        return files;
    }

    private BasicNextMoveFile getArkivmeldingFile(PutMessageRequestWrapper message) throws JAXBException {
        Arkivmelding arkivmelding = arkivmeldingFactory.from(message);
        byte[] arkivmeldingBytes = ArkivmeldingUtil.marshalArkivmelding(arkivmelding);
        return BasicNextMoveFile.of(ARKIVMELDING_FILE,
                ARKIVMELDING_FILE, MimeTypeExtensionMapper.getMimetype("xml"), arkivmeldingBytes);
    }
}
