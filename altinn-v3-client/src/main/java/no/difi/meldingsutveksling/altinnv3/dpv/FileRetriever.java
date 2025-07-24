package no.difi.meldingsutveksling.altinnv3.dpv;

import lombok.RequiredArgsConstructor;
import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.nextmove.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "difi.move.feature.enableDPV", havingValue = "true")
@RequiredArgsConstructor
public class FileRetriever {

    private final OptionalCryptoMessagePersister optionalCryptoMessagePersister;
    private final ArkivmeldingUtil arkivmeldingUtil;
    private final DpvHelper dpvHelper;

    public List<FileUploadRequest> getFiles(NextMoveOutMessage message) {
        if (message.getBusinessMessage() instanceof ArkivmeldingMessage) {
            return getArkivmeldingFiles(message);
        }

        if (message.getBusinessMessage() instanceof DigitalDpvMessage) {
            return getDigitalDpvFiles(message);
        }

        throw new CorrespondenceApiException("Can not fetch files. StandardBusinessDocument.any not instance of %s or %s, aborting".formatted(
            ArkivmeldingMessage.class.getName(), DigitalDpvMessage.class.getName()));
    }

    private List<FileUploadRequest> getArkivmeldingFiles(NextMoveOutMessage message){
        Map<String, BusinessMessageFile> fileMap = message.getFiles().stream()
            .collect(Collectors.toMap(BusinessMessageFile::getFilename, p -> p));

        Arkivmelding arkivmelding = dpvHelper.getArkivmelding(message, fileMap);

        List<BusinessMessageFile> files = arkivmeldingUtil.getFilenames(arkivmelding)
            .stream()
            .map(fileMap::get)
            .filter(Objects::nonNull).collect(Collectors.toList());

        return getAttachments(message.getMessageId(), files);
    }

    private List<FileUploadRequest> getDigitalDpvFiles(NextMoveMessage message){
        return getAttachments(message.getMessageId(), message.getFiles());
    }

    private List<FileUploadRequest> getAttachments(String messageId, Collection<BusinessMessageFile> files) {
        return files
            .stream()
            .sorted(Comparator.comparing(BusinessMessageFile::getDokumentnummer))
            .map(file -> {
                Resource resource = getResource(messageId, file);
                return new FileUploadRequest(file, resource);
            })
            .collect(Collectors.toList());
    }

    private Resource getResource(String messageId, BusinessMessageFile f) {
        try {
            return optionalCryptoMessagePersister.read(messageId, f.getIdentifier());
        } catch (IOException e) {
            throw new CorrespondenceApiException("Could read file named '%s' for messageId=%s".formatted(
                f.getIdentifier(), f.getFilename()), e);
        }
    }

}
