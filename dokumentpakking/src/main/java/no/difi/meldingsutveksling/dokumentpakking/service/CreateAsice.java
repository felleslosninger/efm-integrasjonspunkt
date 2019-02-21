package no.difi.meldingsutveksling.dokumentpakking.service;

import no.difi.asic.AsicWriter;
import no.difi.asic.AsicWriterFactory;
import no.difi.asic.MimeType;
import no.difi.asic.SignatureHelper;
import no.difi.meldingsutveksling.dokumentpakking.domain.Archive;
import no.difi.meldingsutveksling.dokumentpakking.domain.Manifest;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.ByteArrayFile;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.StreamedFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import static java.util.Collections.singletonList;


public class CreateAsice {

    ManifestFactory manifestFactory;

    public CreateAsice() {
        manifestFactory = new ManifestFactory();
    }

    public Archive createAsice(ByteArrayFile forsendelse, SignatureHelper signatureHelper, Avsender avsender,
                               Mottaker mottaker) throws IOException {
        return createAsice(singletonList(forsendelse), signatureHelper, avsender, mottaker);
    }

    public void createAsiceStreamed(List<StreamedFile> files, OutputStream archive, SignatureHelper signatureHelper, Avsender avsender,
                                    Mottaker mottaker) throws IOException {

        Manifest manifest = manifestFactory.createManifest(avsender.getOrgNummer(), mottaker.getOrgNummer(),
                files.isEmpty() ? null : files.get(0).getFileName(), files.get(0).getMimeType());
        AsicWriter asicWriter = AsicWriterFactory.newFactory()
                .newContainer(archive)
                .add(new ByteArrayInputStream(manifest.getBytes()), "manifest.xml");
        for (StreamedFile f : files) {
            asicWriter.add(f.getInputStream(), f.getFileName(), MimeType.forString(f.getMimeType()));
        }

        asicWriter.sign(signatureHelper);
    }

    public Archive createAsice(List<ByteArrayFile> forsendelse, SignatureHelper signatureHelper, Avsender avsender,
                               Mottaker mottaker) throws IOException {

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
