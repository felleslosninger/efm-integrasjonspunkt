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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


public class CreateAsice {

    ManifestFactory manifestFactory;

    public CreateAsice() {
        manifestFactory = new ManifestFactory();
    }

    public Archive createAsice(ByteArrayFile forsendelse, SignatureHelper signatureHelper, Avsender avsender,
                               Mottaker mottaker) throws IOException {
        return createAsice(Arrays.asList(forsendelse), signatureHelper, avsender, mottaker);
    }

    public Archive createAsice(List<ByteArrayFile> forsendelse, SignatureHelper signatureHelper, Avsender avsender,
                               Mottaker mottaker) throws IOException {

        Manifest manifest = manifestFactory.createManifest(avsender.getOrgNummer(), mottaker.getOrgNummer(),
                forsendelse.isEmpty() ? null : forsendelse.get(0));
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
