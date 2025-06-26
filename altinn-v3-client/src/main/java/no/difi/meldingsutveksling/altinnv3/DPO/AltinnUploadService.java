package no.difi.meldingsutveksling.altinnv3.DPO;

import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.altinnv3.DPO.altinn2.AltinnPackage;
import no.difi.meldingsutveksling.altinnv3.DPO.altinn2.shipping.UploadRequest;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.move.common.io.OutputStreamResource;
import no.difi.move.common.io.pipe.Plumber;
import no.difi.move.common.io.pipe.PromiseMaker;
import no.difi.move.common.io.pipe.Reject;
import no.digdir.altinn3.broker.model.FileTransferInitalizeExt;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
//@ConditionalOnProperty(name = "difi.move.feature.enableDPO", havingValue = "true")
@RequiredArgsConstructor
public class AltinnUploadService {

    @Inject
    IntegrasjonspunktProperties integrasjonspunktProperties;

    private final BrokerApiClient brokerApiClient;
    private static final String FILE_NAME = "sbd.zip";
    private final PromiseMaker promiseMaker;
    private final Plumber plumber;
    private final ApplicationContext context;
    private final IntegrasjonspunktProperties props;

    public void send(final StandardBusinessDocument sbd){
        send(sbd, null);
    }

    public void send(final StandardBusinessDocument sbd, Resource asic) {

        var sendersReference = getSendersReference(sbd);

        UploadRequest request = new UploadRequest(sendersReference, sbd, asic);

        FileTransferInitalizeExt fileTransferInitalizeExt = createFileTransferInitalizeExt(sbd);
        fileTransferInitalizeExt.setSendersFileTransferReference(sendersReference);

        try {
            promiseMaker.promise(reject -> {
                InputStreamResource altinnZip = getAltinnZip(request, reject);
                try {
                    brokerApiClient.send(fileTransferInitalizeExt, altinnZip.getInputStream().readAllBytes());
                } catch (IOException e) {
                    throw new BrokerApiException("Send failed", e); // todo bedre exception
                }
                return null;
            }).await();
        } catch (Exception e) {
            auditError(request, e);
            throw e;
        }
    }

    private FileTransferInitalizeExt createFileTransferInitalizeExt(final StandardBusinessDocument sbd){
        FileTransferInitalizeExt fileTransferInitalizeExt = new FileTransferInitalizeExt();
        fileTransferInitalizeExt.setRecipients(List.of(sbd.getReceiverIdentifier().getIdentifier()));
        fileTransferInitalizeExt.setFileName(FILE_NAME);
        fileTransferInitalizeExt.setResourceId("eformidling-meldingsteneste-test"); // todo Skal denne vere i properties eller fra sr eller no?
        fileTransferInitalizeExt.setSender(sbd.getSenderIdentifier().getIdentifier());
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
        return UUID.randomUUID().toString();
    }

    private InputStreamResource getAltinnZip(UploadRequest request, Reject reject) {
        return new InputStreamResource(plumber.pipe("write Altinn zip",
            inlet -> {
                AltinnPackage altinnPackage = AltinnPackage.from(request);
                writeAltinnZip(request, altinnPackage, new OutputStreamResource(inlet));
            }, reject).outlet());
    }

    private void writeAltinnZip(UploadRequest request, AltinnPackage altinnPackage, WritableResource writableResource) {
        try {
            altinnPackage.write(writableResource, context);
        } catch (IOException e) {
            auditError(request, e);
            throw new BrokerApiException("Failed to upload a message to Altinn broker service", e);
        }
    }

    private void auditError(UploadRequest request, Exception e) {
        Audit.error("Message failed to upload to altinn", request.getMarkers(), e);
    }

    private boolean isNullOrEmpty(String s) { return s == null || s.isEmpty(); }
}
