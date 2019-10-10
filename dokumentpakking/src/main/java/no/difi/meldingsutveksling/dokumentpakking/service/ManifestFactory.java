package no.difi.meldingsutveksling.dokumentpakking.service;

import no.difi.meldingsutveksling.dokumentpakking.xml.*;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;


class ManifestFactory {

    no.difi.meldingsutveksling.dokumentpakking.domain.Manifest createManifest(NextMoveOutMessage message, String fileName, String mimeType) {

        Avsender avsender = new Avsender(new Organisasjon(Organisasjonsnummer.from(message.getSenderIdentifier())));
        Mottaker mottaker = new Mottaker(new Organisasjon(Organisasjonsnummer.from(message.getReceiverIdentifier())));
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
