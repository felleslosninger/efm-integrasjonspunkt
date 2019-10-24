package no.difi.meldingsutveksling.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.pipes.Plumber;
import no.difi.meldingsutveksling.pipes.PromiseMaker;
import no.difi.meldingsutveksling.pipes.Reject;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static no.difi.meldingsutveksling.NextMoveConsts.ALTINN_SBD_FILE;
import static no.difi.meldingsutveksling.NextMoveConsts.ASIC_FILE;
import static no.difi.meldingsutveksling.pipes.PipeOperations.copyTo;

@Component
@Profile("cucumber")
@RequiredArgsConstructor
public class AltinnZipFactory {

    private final ObjectMapper objectMapper;
    private final AsicFactory asicFactory;
    private final IntegrasjonspunktNokkel keyInfo;
    private final ObjectProvider<CmsUtil> cmsUtilProvider;
    private final Plumber plumber;
    private final PromiseMaker promiseMaker;

    @SneakyThrows
    InputStream createAltinnZip(Message message) {
        byte[] zipAsBytes = getAltinnZipAsBytes(message);
        return new ByteArrayInputStream(zipAsBytes);
    }

    byte[] getAltinnZipAsBytes(Message message) {
        return promiseMaker.promise(reject -> {
            try {
                return getBytes(message, reject);
            } catch (IOException e) {
                throw new RuntimeException("Failed to get AltinnZip", e);
            }
        }).await();
    }

    private byte[] getBytes(Message message, Reject reject) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream out = new ZipOutputStream(bos);
        out.putNextEntry(new ZipEntry(ALTINN_SBD_FILE));
        out.write(objectMapper.writeValueAsString(message.getSbd()).getBytes());
        out.closeEntry();

        out.putNextEntry(new ZipEntry(ASIC_FILE));

        plumber.pipe("create asic", inlet -> asicFactory.createAsic(message, inlet), reject)
                .andThen("CMS encrypt", (outlet, inlet) -> cmsUtilProvider.getIfAvailable().createCMSStreamed(outlet, inlet, keyInfo.getX509Certificate()))
                .andFinally(copyTo(new BufferedOutputStream(out)));

        out.closeEntry();
        out.close();

        return bos.toByteArray();
    }
}
