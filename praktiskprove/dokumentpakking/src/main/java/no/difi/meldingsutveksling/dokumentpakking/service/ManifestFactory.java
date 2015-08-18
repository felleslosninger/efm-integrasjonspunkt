package no.difi.meldingsutveksling.dokumentpakking.service;

import no.difi.meldingsutveksling.dokumentpakking.xml.*;
import no.difi.meldingsutveksling.domain.ByteArrayFile;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;

import java.io.ByteArrayOutputStream;


public class ManifestFactory {

	public  no.difi.meldingsutveksling.dokumentpakking.domain.Manifest createManifest(Organisasjonsnummer avsenderOrg, Organisasjonsnummer mottakerOrg, ByteArrayFile hoveddokument){
		
		Avsender avsender = new Avsender(new Organisasjon(avsenderOrg));
		Mottaker mottaker = new Mottaker(new Organisasjon(mottakerOrg));
		HovedDokument hoveddokumentXml = new HovedDokument(hoveddokument.getFileName(), hoveddokument.getMimeType(), "Hoveddokument", "no");
		
		Manifest xmlManifest = new Manifest(mottaker, avsender, hoveddokumentXml);
		
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		MarshalManifest.marshal(xmlManifest, os);
		return new no.difi.meldingsutveksling.dokumentpakking.domain.Manifest(os.toByteArray());
	}
}
