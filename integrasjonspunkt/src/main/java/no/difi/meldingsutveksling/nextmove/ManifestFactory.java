package no.difi.meldingsutveksling.nextmove;

import no.difi.meldingsutveksling.dokumentpakking.domain.AsicEAttachable;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.manifest.xml.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

@Component
public class ManifestFactory {

    public no.difi.meldingsutveksling.dokumentpakking.domain.Manifest createManifest(NextMoveMessage message, AsicEAttachable mainDocument) {

        Avsender avsender = new Avsender(new Organisasjon(message.getSender().cast(Iso6523.class)));
        Mottaker mottaker = new Mottaker(new Organisasjon(message.getReceiver().cast(Iso6523.class)));
        no.difi.meldingsutveksling.manifest.xml.Manifest xmlManifest;
        if (mainDocument != null) {
            HovedDokument hoveddokumentXml = new HovedDokument(mainDocument.getFilename(),
                    mainDocument.getMimeType().toString(), "Hoveddokument", "no");
            xmlManifest = new Manifest(mottaker, avsender, hoveddokumentXml);
        } else {
            xmlManifest = new Manifest(mottaker, avsender, null);
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        MarshalManifest.marshal(xmlManifest, os);
        return no.difi.meldingsutveksling.dokumentpakking.domain.Manifest.builder()
                .resource(new ByteArrayResource(os.toByteArray()))
                .mimeType(MediaType.APPLICATION_XML)
                .build();
    }

}
