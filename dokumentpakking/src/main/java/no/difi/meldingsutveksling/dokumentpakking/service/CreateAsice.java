package no.difi.meldingsutveksling.dokumentpakking.service;

import lombok.extern.slf4j.Slf4j;
import no.difi.asic.AsicWriter;
import no.difi.asic.AsicWriterFactory;
import no.difi.asic.MimeType;
import no.difi.asic.SignatureHelper;
import no.difi.meldingsutveksling.dokumentpakking.domain.Archive;
import no.difi.meldingsutveksling.dokumentpakking.domain.Manifest;
import no.difi.meldingsutveksling.domain.*;
import no.difi.meldingsutveksling.noarkexchange.StatusMessage;

import java.io.*;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;

@Slf4j
public class CreateAsice {

    ManifestFactory manifestFactory;

    public CreateAsice() {
        manifestFactory = new ManifestFactory();
    }

    public Archive createAsice(ByteArrayFile forsendelse, SignatureHelper signatureHelper, Avsender avsender,
                               Mottaker mottaker) throws IOException {
        return createAsice(singletonList(forsendelse), signatureHelper, avsender, mottaker);
    }

    public void createAsiceStreamed(StreamedFile mainAttachment, Stream<? extends StreamedFile> files, OutputStream archive, SignatureHelper signatureHelper, Avsender avsender,
                                    Mottaker mottaker) throws IOException {
        Manifest manifest = manifestFactory.createManifest(avsender.getOrgNummer(), mottaker.getOrgNummer(),
                mainAttachment.getFileName(), mainAttachment.getMimeType());
        AsicWriter asicWriter = AsicWriterFactory.newFactory()
                .newContainer(archive)
                .add(new ByteArrayInputStream(manifest.getBytes()), "manifest.xml", MimeType.XML);
        files.forEach(f -> {
            try {
                InputStream inputStream = new BufferedInputStream(f.getInputStream());
                asicWriter.add(inputStream, f.getFileName(), MimeType.forString(f.getMimeType()));
            } catch (IOException e) {
                throw new MeldingsUtvekslingRuntimeException(StatusMessage.UNABLE_TO_CREATE_STANDARD_BUSINESS_DOCUMENT.getTechnicalMessage(), e);
            }
        });
        log.info("Before sign");
        asicWriter.sign(signatureHelper);
        log.info("After sign");
    }

    public Archive createAsice(List<ByteArrayFile> forsendelse, SignatureHelper signatureHelper, Avsender
            avsender, Mottaker mottaker) throws IOException {

        Manifest manifest = manifestFactory.createManifest(avsender.getOrgNummer(), mottaker.getOrgNummer(),
                forsendelse.isEmpty() ? null : forsendelse.get(0).getFileName(), forsendelse.get(0).getMimeType());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        AsicWriter asicWriter = AsicWriterFactory.newFactory()
                .newContainer(bos)
                .add(new ByteArrayInputStream(manifest.getBytes()), "manifest.xml");
        for (ByteArrayFile f : forsendelse) {
            asicWriter.add(new ByteArrayInputStream(f.getBytes()), f.getFileName(), MimeType.forString(f.getMimeType()));
        }

        asicWriter.sign(signatureHelper);
        return new Archive(bos.toByteArray());
    }


}
