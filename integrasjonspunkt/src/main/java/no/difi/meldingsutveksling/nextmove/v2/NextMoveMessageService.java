package no.difi.meldingsutveksling.nextmove.v2;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.difi.meldingsutveksling.NextMoveConsts;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingException;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.*;
import no.difi.meldingsutveksling.nextmove.BusinessMessageFile;
import no.difi.meldingsutveksling.nextmove.NextMoveMessage;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

@Component
@RequiredArgsConstructor
public class NextMoveMessageService {

    private final MessagePersister messagePersister;

    StandardBusinessDocument setDefaults(StandardBusinessDocument sbd) {
        sbd.getConversationScope().ifPresent(s -> {
            if (isNullOrEmpty(s.getInstanceIdentifier())) {
                s.setInstanceIdentifier(createConversationId());
            }
        });
        return sbd;
    }

    void validate(NextMoveMessage message) {
        // Must always be atleast one attachment
        if (message.getFiles() == null || message.getFiles().isEmpty()) {
            throw new MissingFileException();
        }

        if (ServiceIdentifier.DPO == message.getServiceIdentifier()) {
            // Arkivmelding must exist for DPO
            BusinessMessageFile arkivmeldingFile = message.getFiles().stream()
                    .filter(f -> NextMoveConsts.ARKIVMELDING_FILE.equals(f.getFilename()))
                    .findAny()
                    .orElseThrow(MissingArkivmeldingException::new);

            InputStream is = messagePersister.readStream(message.getConversationId(), arkivmeldingFile.getIdentifier()).getInputStream();
            Arkivmelding arkivmelding;
            try {
                arkivmelding = ArkivmeldingUtil.unmarshalArkivmelding(is);
            } catch (JAXBException e) {
                throw new UnmarshalArkivmeldingException();
            }

            // Verify each file referenced in arkivmelding is uploaded
            List<String> arkivmeldingFiles;
            try {
                arkivmeldingFiles = ArkivmeldingUtil.getFilenames(arkivmelding);
            } catch (ArkivmeldingException e) {
                throw new ArkivmeldingProcessingException(e);
            }
            Set<String> messageFiles = message.getFiles().stream()
                    .map(BusinessMessageFile::getFilename)
                    .collect(Collectors.toSet());
            List<String> missingFiles = Lists.newArrayList();
            arkivmeldingFiles.forEach(f -> {
                if (!messageFiles.contains(f)) {
                    missingFiles.add(f);
                }
            });
            if (!missingFiles.isEmpty()) {
                throw new MissingArkivmeldingFileException(String.join(",", missingFiles));
            }
        }
    }

    private String createConversationId() {
        return UUID.randomUUID().toString();
    }
}
