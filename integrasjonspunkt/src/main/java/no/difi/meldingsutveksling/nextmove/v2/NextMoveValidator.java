package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.difi.meldingsutveksling.ApiType;
import no.difi.meldingsutveksling.DocumentType;
import no.difi.meldingsutveksling.NextMoveConsts;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.*;
import no.difi.meldingsutveksling.nextmove.BusinessMessageFile;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.nextmove.TimeToLiveHelper;
import no.difi.meldingsutveksling.nextmove.message.FileEntryStream;
import no.difi.meldingsutveksling.nextmove.message.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.pipes.PromiseMaker;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.meldingsutveksling.validation.Asserter;
import no.difi.meldingsutveksling.validation.group.ValidationGroupFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static no.difi.meldingsutveksling.DocumentType.ARKIVMELDING;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPI;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPV;


@Component
@RequiredArgsConstructor
public class NextMoveValidator {

    private final NextMoveServiceRecordProvider serviceRecordProvider;
    private final NextMoveMessageOutRepository messageRepo;
    private final ServiceIdentifierService serviceIdentifierService;
    private final Asserter asserter;
    private final OptionalCryptoMessagePersister optionalCryptoMessagePersister;
    private final TimeToLiveHelper timeToLiveHelper;
    private final SBDUtil sbdUtil;
    private final ConversationService conversationService;
    private final PromiseMaker promiseMaker;

    void validate(StandardBusinessDocument sbd) {
        sbd.getOptionalMessageId().ifPresent(messageId -> {
                    messageRepo.findByMessageId(messageId)
                            .map(p -> {
                                throw new MessageAlreadyExistsException(messageId);
                            });
                    if (!sbdUtil.isStatus(sbd)) {
                        conversationService.findConversation(messageId)
                                .map(c -> {
                                    throw new MessageAlreadyExistsException(messageId);
                                });
                    }
                }
        );

        ServiceRecord serviceRecord = serviceRecordProvider.getServiceRecord(sbd);
        ServiceIdentifier serviceIdentifier = serviceRecord.getServiceIdentifier();

        if (!serviceIdentifierService.isEnabled(serviceIdentifier)) {
            throw new ServiceNotEnabledException(serviceIdentifier);
        }

        DocumentType documentType = DocumentType.valueOf(sbd.getMessageType(), ApiType.NEXTMOVE)
                .orElseThrow(() -> new UnknownNextMoveDocumentTypeException(sbd.getMessageType()));

        String standard = sbd.getStandard();

        if (!documentType.fitsDocumentIdentifier(standard)) {
            throw new DocumentTypeDoNotFitDocumentStandardException(documentType, standard);
        }

        if (!serviceRecord.hasStandard(standard)) {
            throw new ReceiverDoNotAcceptDocumentStandard(standard, sbd.getProcess());
        }

        Class<?> group = ValidationGroupFactory.toServiceIdentifier(serviceIdentifier);
        asserter.isValid(sbd.getAny(), group != null ? new Class<?>[]

                {
                        group
                } : new Class<?>[0]);
    }

    @Transactional(noRollbackFor = TimeToLiveException.class)
    public void validate(NextMoveOutMessage message) {
        // Must always be at least one attachment
        StandardBusinessDocument sbd = message.getSbd();
        if (!sbdUtil.isReceipt(sbd) && (message.getFiles() == null || message.getFiles().isEmpty())) {
            throw new MissingFileException();
        }

        sbd.getExpectedResponseDateTime().ifPresent(expectedResponseDateTime -> {
            if (sbdUtil.isExpired(sbd)) {
                timeToLiveHelper.registerErrorStatusAndMessage(sbd, message.getServiceIdentifier(), message.getDirection());
                throw new TimeToLiveException(expectedResponseDateTime);
            }
        });

        if (sbdUtil.isType(message.getSbd(), ARKIVMELDING)) {
            // Verify each file referenced in arkivmelding is uploaded
            List<String> arkivmeldingFiles = ArkivmeldingUtil.getFilenames(getArkivmelding(message));
            Set<String> messageFiles = message.getFiles().stream()
                    .map(BusinessMessageFile::getFilename)
                    .collect(Collectors.toSet());

            List<String> missingFiles = arkivmeldingFiles.stream()
                    .filter(p -> !messageFiles.contains(p))
                    .collect(Collectors.toList());

            if (!missingFiles.isEmpty()) {
                throw new MissingArkivmeldingFileException(String.join(",", missingFiles));
            }
        }

        if (message.getServiceIdentifier() == DPI && message.getFiles().stream()
                .noneMatch(BusinessMessageFile::getPrimaryDocument)) {
            throw new MissingPrimaryDocumentException();
        }
    }

    private Arkivmelding getArkivmelding(NextMoveOutMessage message) {
        // Arkivmelding must exist for DPO
        BusinessMessageFile arkivmeldingFile = message.getFiles().stream()
                .filter(f -> NextMoveConsts.ARKIVMELDING_FILE.equals(f.getFilename()))
                .findAny()
                .orElseThrow(MissingArkivmeldingException::new);

        return promiseMaker.await(reject -> {
            try (FileEntryStream fileEntryStream = optionalCryptoMessagePersister.readStream(message.getMessageId(), arkivmeldingFile.getIdentifier(), reject)) {
                return ArkivmeldingUtil.unmarshalArkivmelding(fileEntryStream.getInputStream());
            } catch (JAXBException | IOException e) {
                throw new UnmarshalArkivmeldingException();
            }
        });
    }

    void validateFile(NextMoveOutMessage message, MultipartFile file) {
        Set<BusinessMessageFile> files = message.getOrCreateFiles();

        if (message.isPrimaryDocument(file.getOriginalFilename()) && files.stream().anyMatch(BusinessMessageFile::getPrimaryDocument)) {
            throw new MultiplePrimaryDocumentsNotAllowedException();
        }

        List<ServiceIdentifier> requiredTitleCapabilities = asList(DPV, DPI);
        if (requiredTitleCapabilities.contains(message.getServiceIdentifier())
                && !StringUtils.hasText(file.getName())) {
            throw new MissingFileTitleException(requiredTitleCapabilities.stream()
                    .map(ServiceIdentifier::toString)
                    .collect(Collectors.joining(",")));
        }
    }
}
