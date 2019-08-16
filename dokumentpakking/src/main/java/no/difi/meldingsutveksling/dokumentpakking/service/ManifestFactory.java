package no.difi.meldingsutveksling.dokumentpakking.service;

import no.difi.meldingsutveksling.dokumentpakking.xml.*;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;


class ManifestFactory {

    no.difi.meldingsutveksling.dokumentpakking.domain.Manifest createManifest(Organisasjonsnummer avsenderOrg, Organisasjonsnummer mottakerOrg, String fileName, String mimeType) {

        Avsender avsender = new Avsender(new Organisasjon(avsenderOrg));
        Mottaker mottaker = new Mottaker(new Organisasjon(mottakerOrg));
        Manifest xmlManifest;
        if (StringUtils.hasText(fileName) && StringUtils.hasText(mimeType)) {
            HovedDokument hoveddokumentXml = new HovedDokument(fileName, mimeType, "Hoveddokument", "no");
            xmlManifest = new Manifest(mottaker, avsender, hoveddokumentXml);
        } else {
            xmlManifest = new Manifest(mottaker, avsender, null);
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        MarshalManifest.marshal(xmlManifest, os);
        return new no.difi.meldingsutveksling.dokumentpakking.domain.Manifest(os.toByteArray());
    }
}
