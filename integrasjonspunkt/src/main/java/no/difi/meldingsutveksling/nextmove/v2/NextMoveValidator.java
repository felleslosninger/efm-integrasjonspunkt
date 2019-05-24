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
import no.difi.meldingsutveksling.nextmove.message.CryptoMessagePersister;
import no.difi.meldingsutveksling.nextmove.message.FileEntryStream;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.meldingsutveksling.validation.Asserter;
import no.difi.meldingsutveksling.validation.group.ValidationGroupFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Arrays.asList;
import static no.difi.meldingsutveksling.DocumentType.ARKIVMELDING;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPI;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPV;
import static no.difi.meldingsutveksling.nextmove.ConversationDirection.OUTGOING;


@Component
@RequiredArgsConstructor
public class NextMoveValidator {

    private final NextMoveServiceRecordProvider serviceRecordProvider;
    private final NextMoveMessageOutRepository messageRepo;
    private final ServiceIdentifierService serviceIdentifierService;
    private final Asserter asserter;
    private final CryptoMessagePersister cryptoMessagePersister;
    private final TimeToLiveHelper timeToLiveHelper;
    private final SBDUtil sbdUtil;

    void validate(StandardBusinessDocument sbd) {
        sbd.getOptionalConversationId()
                .flatMap(messageRepo::findByConversationId)
                .map(p -> {
                    throw new ConversationAlreadyExistsException(p.getConversationId());
                });

        ServiceRecord serviceRecord = serviceRecordProvider.getServiceRecord(sbd);
        ServiceIdentifier serviceIdentifier = serviceRecord.getServiceIdentifier();

        if (!serviceIdentifierService.isEnabled(serviceIdentifier)) {
            throw new ServiceNotEnabledException(serviceIdentifier);
        }

        DocumentType documentType = DocumentType.valueOf(sbd.getMessageType(), ApiType.NEXTMOVE)
                .orElseThrow(() -> new UnknownNextMoveDocumentTypeException(sbd.getMessageType()));

        String standard = sbd.getStandard();

        if (!documentType.fitsStandard(standard)) {
            throw new DocumentTypeDoNotFitDocumentStandardException(documentType, standard);
        }

        if (!serviceRecord.hasStandard(standard)) {
            throw new ReceiverDoNotAcceptDocumentStandard(standard, sbd.getProcess());
        }

        if (sbdUtil.isExpired(sbd)) {
            timeToLiveHelper.registerErrorStatusAndMessage(sbd, serviceIdentifier, OUTGOING);
            throw new TimeToLiveException(sbd.getExpectedResponseDateTime());
        }

        Class<?> group = ValidationGroupFactory.toServiceIdentifier(serviceIdentifier);
        asserter.isValid(sbd.getAny(), group != null ? new Class<?>[]{group} : new Class<?>[0]);
    }

    void validate(NextMoveOutMessage message) {
        // Must always be at least one attachment
        StandardBusinessDocument sbd = message.getSbd();
        if (!sbdUtil.isReceipt(sbd) && (message.getFiles() == null || message.getFiles().isEmpty())) {
            throw new MissingFileException();
        }

        if (sbdUtil.isExpired(sbd)) {
            timeToLiveHelper.registerErrorStatusAndMessage(sbd, message.getServiceIdentifier(), message.getDirection());
            throw new TimeToLiveException(sbd.getExpectedResponseDateTime());
        }

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
    }

    private Arkivmelding getArkivmelding(NextMoveOutMessage message) {
        // Arkivmelding must exist for DPO
        BusinessMessageFile arkivmeldingFile = message.getFiles().stream()
                .filter(f -> NextMoveConsts.ARKIVMELDING_FILE.equals(f.getFilename()))
                .findAny()
                .orElseThrow(MissingArkivmeldingException::new);

        try (FileEntryStream fileEntryStream = cryptoMessagePersister.readStream(message.getConversationId(), arkivmeldingFile.getIdentifier())) {
            return ArkivmeldingUtil.unmarshalArkivmelding(fileEntryStream.getInputStream());
        } catch (JAXBException | IOException e) {
            throw new UnmarshalArkivmeldingException();
        }
    }

    void validateFile(NextMoveOutMessage message, MultipartFile file) {
        Set<BusinessMessageFile> files = message.getOrCreateFiles();

        if (message.isPrimaryDocument(file.getOriginalFilename()) && files.stream().anyMatch(BusinessMessageFile::getPrimaryDocument)) {
            throw new MultiplePrimaryDocumentsNotAllowedException();
        }

        List<ServiceIdentifier> requiredTitleCapabilities = asList(DPV, DPI);
        if (requiredTitleCapabilities.contains(message.getServiceIdentifier()) && isNullOrEmpty(file.getName())) {
            throw new MissingFileTitleException(requiredTitleCapabilities.stream()
                    .map(ServiceIdentifier::toString)
                    .collect(Collectors.joining(",")));
        }
    }
}
