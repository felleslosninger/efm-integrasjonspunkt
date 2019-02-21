package no.difi.meldingsutveksling.dokumentpakking.service;

import com.google.common.base.Strings;
import no.difi.meldingsutveksling.dokumentpakking.xml.*;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;

import java.io.ByteArrayOutputStream;


public class ManifestFactory {

	public  no.difi.meldingsutveksling.dokumentpakking.domain.Manifest createManifest(Organisasjonsnummer avsenderOrg, Organisasjonsnummer mottakerOrg, String fileName, String mimeType){
		
		Avsender avsender = new Avsender(new Organisasjon(avsenderOrg));
		Mottaker mottaker = new Mottaker(new Organisasjon(mottakerOrg));
		Manifest xmlManifest;
		if (!Strings.isNullOrEmpty(fileName) && !Strings.isNullOrEmpty(mimeType)) {
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
