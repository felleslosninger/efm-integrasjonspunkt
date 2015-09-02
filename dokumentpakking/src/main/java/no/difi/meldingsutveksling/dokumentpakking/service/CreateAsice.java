package no.difi.meldingsutveksling.dokumentpakking.service;

import no.difi.asic.AsicWriter;
import no.difi.asic.AsicWriterFactory;
import no.difi.asic.SignatureHelper;
import no.difi.meldingsutveksling.dokumentpakking.domain.Archive;
import no.difi.meldingsutveksling.dokumentpakking.domain.Manifest;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.ByteArrayFile;
import no.difi.meldingsutveksling.domain.Mottaker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class CreateAsice {

    ManifestFactory manifestFactory;

    public CreateAsice() {
        manifestFactory = new ManifestFactory();
    }

    public Archive createAsice(ByteArrayFile forsendelse, SignatureHelper signatureHelper, Avsender avsender, Mottaker mottaker) throws IOException {

        Manifest manifest = manifestFactory.createManifest(avsender.getOrgNummer(), mottaker.getOrgNummer(), forsendelse);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        AsicWriter asicWriter = AsicWriterFactory.newFactory()
                .newContainer(bos)
                .add(new ByteArrayInputStream(forsendelse.getBytes()), "edu_test.xml")
                .add(new ByteArrayInputStream(manifest.getBytes()), "manifest.xml");
        asicWriter.sign(signatureHelper);
        Archive result = new Archive(bos.toByteArray());
        return result;
    }


}
