package no.difi.meldingsutveksling.altinnv3.dpo.payload;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.TmpFile;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.BusinessMessageAsAttachment;
import no.difi.meldingsutveksling.altinnv3.dpo.UploadRequest;
import no.difi.move.common.io.ResourceUtils;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.util.StreamUtils;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static no.difi.meldingsutveksling.NextMoveConsts.ASIC_FILE;
import static no.difi.meldingsutveksling.NextMoveConsts.SBD_FILE;

@Slf4j
public class AltinnPackage {
    private static final String CONTENT_XML = "content.xml";

    private static final JAXBContext ctx;
    private final StandardBusinessDocument sbd;
    private final Resource asic;
    private final TmpFile tmpFile;

    static {
        try {
            ctx = JAXBContextFactory.createContext(new Class[]{StandardBusinessDocument.class}, new HashMap());
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException("Could not create JAXBContext", e);
        }
    }

    private AltinnPackage(StandardBusinessDocument sbd, Resource asic, TmpFile tmpFile) {
        this.sbd = sbd;
        this.asic = asic;
        this.tmpFile = tmpFile;
    }

    public static AltinnPackage from(UploadRequest document) {
        return new AltinnPackage(document.getPayload(), document.getAsic(), null);
    }

    /**
     * Writes the Altinn package as a Zip file
     *
     * @param writableResource where the Zip file is written
     * @param context          {@link ApplicationContext}
     * @throws IOException
     */
    public void write(WritableResource writableResource, ApplicationContext context) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(writableResource.getOutputStream())) {
            if (sbd.getAny() instanceof BusinessMessageAsAttachment) {
                zipOutputStream.putNextEntry(new ZipEntry(SBD_FILE));
                ObjectMapper om = context.getBean(ObjectMapper.class);
                om.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
                om.writeValue(zipOutputStream, sbd);
                zipOutputStream.closeEntry();

                if (this.asic != null) {
                    zipOutputStream.putNextEntry(new ZipEntry(ASIC_FILE));
                    ResourceUtils.copy(this.asic, zipOutputStream);
                    zipOutputStream.closeEntry();
                }
            }

            zipOutputStream.flush();
        }
    }

    public static AltinnPackage from(File f, ApplicationContext context) throws IOException, JAXBException {
        try (ZipFile zipFile = new ZipFile(f)) {
            Unmarshaller unmarshaller = ctx.createUnmarshaller();

            StandardBusinessDocument sbd = null;
            TmpFile tmpAsicFile = null;
            Resource asic = null;

            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                switch (zipEntry.getName()) {
                    case SBD_FILE:
                        ObjectMapper om = context.getBean(ObjectMapper.class);
                        om.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
                        sbd = om.readValue(zipFile.getInputStream(zipEntry), StandardBusinessDocument.class);
                        break;
                    case CONTENT_XML:
                        Source source = new StreamSource(zipFile.getInputStream(zipEntry));
                        sbd = unmarshaller.unmarshal(source, StandardBusinessDocument.class).getValue();
                        break;
                    case ASIC_FILE:
                        tmpAsicFile = TmpFile.create(zipFile.getInputStream(zipEntry));
                        asic = new FileSystemResource(tmpAsicFile.getFile());
                        break;
                    default:
                        log.info("Skipping file: {}", zipEntry.getName());
                }
            }

            if (sbd == null) {
                throw new MeldingsUtvekslingRuntimeException("Altinn zip does not contain BestEdu document, cannot proceed");
            }
            return new AltinnPackage(sbd, asic, tmpAsicFile);
        }
    }

    public static AltinnPackage from(Resource altinnZip) throws IOException, JAXBException {
        try (ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(altinnZip.getInputStream())) {
            Unmarshaller unmarshaller = ctx.createUnmarshaller();

            ArchiveEntry zipEntry;
            StandardBusinessDocument sbd = null;

            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (zipEntry.getName().equals(CONTENT_XML)) {
                    Source source = new StreamSource(StreamUtils.nonClosing(zipInputStream));
                    sbd = unmarshaller.unmarshal(source, StandardBusinessDocument.class).getValue();
                } else {
                    log.info("Skipping file: {}", zipEntry.getName());
                }
            }
            return new AltinnPackage(sbd, null, null);
        }
    }

    public StandardBusinessDocument getSbd() {
        return this.sbd;
    }

    public Resource getAsic() {
        return this.asic;
    }

    public TmpFile getTmpFile() {
        return this.tmpFile;
    }
}
