package no.difi.meldingsutveksling.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.pipes.Pipe;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static no.difi.meldingsutveksling.NextMoveConsts.ALTINN_SBD_FILE;
import static no.difi.meldingsutveksling.NextMoveConsts.ASIC_FILE;
import static no.difi.meldingsutveksling.pipes.PipeOperations.copy;

@Slf4j
@Component
@Profile("cucumber")
@RequiredArgsConstructor
public class AltinnZipFactory {

    private final ObjectMapper objectMapper;
    private final AsicFactory asicFactory;
    private final IntegrasjonspunktNokkel keyInfo;
    private final ObjectProvider<CmsUtil> cmsUtilProvider;

    @SneakyThrows
    InputStream createAltinnZip(Message message) {
        byte[] zipAsBytes = getAltinnZipAsBytes(message);
        return new ByteArrayInputStream(zipAsBytes);
    }

    byte[] getAltinnZipAsBytes(Message message) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream out = new ZipOutputStream(bos);
        out.putNextEntry(new ZipEntry(ALTINN_SBD_FILE));
        out.write(objectMapper.writeValueAsString(message.getSbd()).getBytes());
        out.closeEntry();

        out.putNextEntry(new ZipEntry(ASIC_FILE));

        log.info("1");

        PipedInputStream encryptedAsic = Pipe.of("Get ASIC", copy(asicFactory.getAsic(message)))
                .andThen("CMS encrypt", (outlet, inlet) -> cmsUtilProvider.getIfAvailable().createCMSStreamed(outlet, inlet, keyInfo.getX509Certificate()))
                .outlet();

        log.info("2");
        IOUtils.copy(encryptedAsic, out);
        log.info("3");
        out.flush();
        log.info("4");
        out.closeEntry();
        log.info("5");
        out.close();
        log.info("6");

        return bos.toByteArray();
    }
}
