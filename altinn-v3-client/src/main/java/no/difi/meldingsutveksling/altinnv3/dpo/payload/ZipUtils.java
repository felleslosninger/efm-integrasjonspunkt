package no.difi.meldingsutveksling.altinnv3.dpo.payload;

import jakarta.xml.bind.JAXBException;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.TmpFile;
import no.difi.meldingsutveksling.altinnv3.dpo.BrokerApiException;
import no.difi.meldingsutveksling.altinnv3.dpo.UploadRequest;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.move.common.io.OutputStreamResource;
import no.difi.move.common.io.pipe.Plumber;
import no.difi.move.common.io.pipe.Reject;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.WritableResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPO", havingValue = "true")
@RequiredArgsConstructor
public class ZipUtils {

    private final Plumber plumber;
    private final ApplicationContext context;

    public InputStreamResource getAltinnZip(UploadRequest request, Reject reject) {
        return new InputStreamResource(plumber.pipe("write Altinn zip",
            inlet -> {
                AltinnPackage altinnPackage = AltinnPackage.from(request);
                writeAltinnZip(request, altinnPackage, new OutputStreamResource(inlet));
            }, reject).outlet());
    }

    public AltinnPackage getAltinnPackage(byte[] bytes) throws JAXBException, IOException {
        TmpFile tmpFile = TmpFile.create();

        try {
            File file = tmpFile.getFile();
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
                FileUtils.copyInputStreamToFile(inputStream, file);
            }
            return AltinnPackage.from(file, context);
        } finally {
            tmpFile.delete();
        }
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

}
