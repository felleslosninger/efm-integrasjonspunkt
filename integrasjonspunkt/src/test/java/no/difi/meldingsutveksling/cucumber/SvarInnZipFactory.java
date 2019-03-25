package no.difi.meldingsutveksling.cucumber;

import lombok.SneakyThrows;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.domain.ByteArrayFile;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class SvarInnZipFactory {

    private final CmsUtil cmsUtil;
    private final IntegrasjonspunktNokkel integrasjonspunktNokkel;

    public SvarInnZipFactory(CmsUtil cmsUtil, IntegrasjonspunktProperties properties) {
        this.cmsUtil = cmsUtil;
        this.integrasjonspunktNokkel = new IntegrasjonspunktNokkel(properties.getFiks().getKeystore());
    }

    @SneakyThrows
    byte[] createSvarInnZip(Message message) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream out = new ZipOutputStream(bos);

        for (ByteArrayFile file : message.getAttachments()) {
            out.putNextEntry(new ZipEntry(file.getFileName()));
            out.write(file.getBytes());
            out.closeEntry();
        }

        out.close();

        return encrypt(bos.toByteArray());
    }

    private byte[] encrypt(byte[] in) {
        return cmsUtil.createCMS(in, integrasjonspunktNokkel.getX509Certificate());
    }

}
