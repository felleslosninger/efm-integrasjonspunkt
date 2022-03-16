package no.difi.meldingsutveksling.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import no.difi.move.common.io.OutputStreamResource;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.FastByteArrayOutputStream;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static no.difi.meldingsutveksling.NextMoveConsts.ASIC_FILE;
import static no.difi.meldingsutveksling.NextMoveConsts.SBD_FILE;

@Component
@Profile("cucumber")
@RequiredArgsConstructor
public class AltinnZipFactory {

    private final ObjectMapper objectMapper;
    private final CreateAsic createAsic;

    public ByteArrayResource createAltinnZip(Message message) throws IOException {
        FastByteArrayOutputStream bos = new FastByteArrayOutputStream();
        ZipOutputStream out = new ZipOutputStream(bos);
        out.putNextEntry(new ZipEntry(SBD_FILE));
        out.write(objectMapper.writeValueAsString(message.getSbd()).getBytes());
        out.closeEntry();

        out.putNextEntry(new ZipEntry(ASIC_FILE));
        createAsic.createAsic(message, new OutputStreamResource(out));

        out.closeEntry();
        out.close();

        return new ByteArrayResource(bos.toByteArray());
    }
}
