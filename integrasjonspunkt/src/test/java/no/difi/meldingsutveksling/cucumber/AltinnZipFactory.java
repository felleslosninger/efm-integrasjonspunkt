package no.difi.meldingsutveksling.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static no.difi.meldingsutveksling.NextMoveConsts.ALTINN_SBD_FILE;
import static no.difi.meldingsutveksling.NextMoveConsts.ASIC_FILE;

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
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream out = new ZipOutputStream(bos);
        out.putNextEntry(new ZipEntry(ALTINN_SBD_FILE));
        out.write(objectMapper.writeValueAsString(message.getSbd()).getBytes());
        out.closeEntry();

        out.putNextEntry(new ZipEntry(ASIC_FILE));

        byte[] asic = asicFactory.getAsic(message);
        byte[] encryptedAsic = cmsUtilProvider.getIfAvailable().createCMS(asic, keyInfo.getX509Certificate());

        out.write(encryptedAsic);
        out.closeEntry();

        out.close();

        return new ByteArrayInputStream(bos.toByteArray());
    }
}
