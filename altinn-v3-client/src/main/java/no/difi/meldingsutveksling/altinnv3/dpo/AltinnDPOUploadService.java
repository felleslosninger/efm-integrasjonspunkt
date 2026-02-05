package no.difi.meldingsutveksling.altinnv3.dpo;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.UUIDGenerator;
import no.difi.meldingsutveksling.altinnv3.dpo.payload.ZipUtils;
import no.difi.meldingsutveksling.config.AltinnSystemUser;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.move.common.io.pipe.PromiseMaker;
import no.digdir.altinn3.broker.model.FileTransferInitalizeExt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyMap;

@Service
@ConditionalOnProperty(name = "difi.move.feature.enableDPO", havingValue = "true")
@RequiredArgsConstructor
public class AltinnDPOUploadService {

    private final BrokerApiClient brokerApiClient;
    private final PromiseMaker promiseMaker;
    private final ZipUtils zipUtils;
    private final IntegrasjonspunktProperties props;
    private static final String FILE_NAME = "sbd.zip";
    private final UUIDGenerator uuidGenerator;

    public void send(final StandardBusinessDocument sbd){
        send(sbd, null);
    }

    public void send(final StandardBusinessDocument sbd, Resource asic) {

        String sendersReference = getSendersReference(sbd);

        UploadRequest request = new UploadRequest(sendersReference, sbd, asic);
        FileTransferInitalizeExt fileTransferInitalizeExt = createFileTransferInitalizeExt(sbd, sendersReference);

        AltinnSystemUser systemUser = getAltinnSystemUserFromSenderOrgId(sbd.getSenderIdentifier().getIdentifier());

        try {
            promiseMaker.promise(reject -> {
                InputStreamResource altinnZip = zipUtils.getAltinnZip(request, reject);
                try {
                    brokerApiClient.send(systemUser, fileTransferInitalizeExt, altinnZip.getInputStream().readAllBytes());
                } catch (IOException e) {
                    throw new BrokerApiException("Failed when trying to send DPO message with conversationId %s".formatted(sbd.getConversationId()), e);
                }
                return null;
            }).await();
        } catch (Exception e) {
            auditError(request, e);
            throw e;
        }

    }

    private AltinnSystemUser getAltinnSystemUserFromSenderOrgId(String senderOrgId) {

        // default er å sende på vegne av seg selv
        AltinnSystemUser systemUser = props.getDpo().getSystemUser();

        // hvis avsender ikke er deg selv, let gjennom reportees etter korrekt systemUser
        if (!systemUser.getOrgId().equals(senderOrgId)) {
            if (props.getDpo().getReportees() == null) {
                throw new BrokerApiException("Sender %s fra SBD matcher ikke systembruker og det mangler reportees i konfigurasjonen".formatted(senderOrgId));
            } else {
                systemUser = props.getDpo().getReportees().stream()
                    .filter(s -> s.getOrgId().equals(senderOrgId))
                    .findFirst().orElseThrow(() -> new BrokerApiException("Sender %s fra SBD matcher ikke konfigurerte systembrukere".formatted(senderOrgId)));
            }
        }

        return systemUser;
    }

    private FileTransferInitalizeExt createFileTransferInitalizeExt(final StandardBusinessDocument sbd, String sendersReference) {
        FileTransferInitalizeExt fileTransferInitalizeExt = new FileTransferInitalizeExt();

        fileTransferInitalizeExt.setRecipients(List.of(sbd.getReceiverIdentifier().getIdentifier()));
        fileTransferInitalizeExt.setFileName(FILE_NAME);
        fileTransferInitalizeExt.setResourceId(props.getDpo().getResource());
        fileTransferInitalizeExt.setSender(sbd.getSenderIdentifier().getIdentifier());
        fileTransferInitalizeExt.setSendersFileTransferReference(sendersReference);
        fileTransferInitalizeExt.setPropertyList(emptyMap());

        return fileTransferInitalizeExt;
    }

    private String getSendersReference(StandardBusinessDocument sbd) {
        Optional<Scope> mcScope = SBDUtil.getOptionalMessageChannel(sbd);
        if (mcScope.isPresent() &&
            (SBDUtil.isStatus(sbd) || SBDUtil.isReceipt(sbd)) &&
            (isNullOrEmpty(props.getDpo().getMessageChannel()) ||
                !mcScope.get().getIdentifier().equals(props.getDpo().getMessageChannel()))) {
            return mcScope.get().getIdentifier();
        }
        return uuidGenerator.generate();
    }

    private void auditError(UploadRequest request, Exception e) {
        Audit.error("Message failed to upload to altinn", request.getMarkers(), e);
    }

    private boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

}
