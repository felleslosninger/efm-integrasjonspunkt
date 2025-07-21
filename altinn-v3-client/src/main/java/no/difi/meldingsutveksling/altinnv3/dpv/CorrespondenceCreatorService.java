package no.difi.meldingsutveksling.altinnv3.dpv;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.arkivverket.standarder.noark5.arkivmelding.Journalpost;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.nextmove.*;
import no.digdir.altinn3.correspondence.model.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
public class CorrespondenceCreatorService {

    private final DpvHelper dpvHelper;
    private final ArkivmeldingUtil arkivmeldingUtil;
    private final CorrespondenceFactory correspondenceFactory;

    @SneakyThrows
    public InitializeCorrespondencesExt create(NextMoveOutMessage message, List<UUID> existingAttachments, List<BusinessMessageFile> newAttachmentsMetaData) {
        if (message.getBusinessMessage() instanceof ArkivmeldingMessage) {
            return handleArkivmeldingMessage(message, existingAttachments, newAttachmentsMetaData);
        }

        if (message.getBusinessMessage() instanceof DigitalDpvMessage) {
            return handleDigitalDpvMessage(message, existingAttachments, newAttachmentsMetaData);
        }

        throw new NextMoveRuntimeException("StandardBusinessDocument.any not instance of %s or %s, aborting".formatted(
            ArkivmeldingMessage.class.getName(), DigitalDpvMessage.class.getName()));
    }

    private InitializeCorrespondencesExt handleDigitalDpvMessage(NextMoveOutMessage message, List<UUID> existingAttachments, List<BusinessMessageFile> newAttachmentsMetaData) {
        DigitalDpvMessage msg = (DigitalDpvMessage) message.getBusinessMessage();

        return correspondenceFactory.create(message,
            msg.getTittel(),
            msg.getSammendrag(),
            msg.getInnhold(),
            existingAttachments,
            newAttachmentsMetaData
        );
    }

    private InitializeCorrespondencesExt handleArkivmeldingMessage(NextMoveOutMessage message, List<UUID> existingAttachments, List<BusinessMessageFile> newAttachmentsMetaData) {
        Map<String, BusinessMessageFile> fileMap = message.getFiles().stream()
            .collect(Collectors.toMap(BusinessMessageFile::getFilename, p -> p));

        Arkivmelding arkivmelding = dpvHelper.getArkivmelding(message, fileMap);
        Journalpost jp = arkivmeldingUtil.getJournalpost(arkivmelding);

        return correspondenceFactory.create(message,
            jp.getOffentligTittel(),
            jp.getOffentligTittel(),
            jp.getTittel(),
            existingAttachments,
            newAttachmentsMetaData
        );
    }
}
