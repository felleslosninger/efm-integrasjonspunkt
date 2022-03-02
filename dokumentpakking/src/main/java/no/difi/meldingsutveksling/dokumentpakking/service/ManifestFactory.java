package no.difi.meldingsutveksling.dokumentpakking.service;

import no.difi.meldingsutveksling.dokumentpakking.xml.*;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.nextmove.NextMoveMessage;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;


class ManifestFactory {

    no.difi.meldingsutveksling.dokumentpakking.domain.Manifest createManifest(NextMoveMessage message, String fileName, String mimeType) {

        Avsender avsender = new Avsender(new Organisasjon(message.getSender().cast(Iso6523.class)));
        Mottaker mottaker = new Mottaker(new Organisasjon(message.getReceiver().cast(Iso6523.class)));
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
