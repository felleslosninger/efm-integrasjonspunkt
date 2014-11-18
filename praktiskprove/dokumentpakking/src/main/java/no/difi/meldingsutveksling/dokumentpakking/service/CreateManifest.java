package no.difi.meldingsutveksling.dokumentpakking.service;

import java.io.ByteArrayOutputStream;

import no.difi.meldingsutveksling.dokumentpakking.xml.Avsender;
import no.difi.meldingsutveksling.dokumentpakking.xml.HovedDokument;
import no.difi.meldingsutveksling.dokumentpakking.xml.Manifest;
import no.difi.meldingsutveksling.dokumentpakking.xml.MarshalManifest;
import no.difi.meldingsutveksling.dokumentpakking.xml.Mottaker;
import no.difi.meldingsutveksling.dokumentpakking.xml.Organisasjon;
import no.difi.meldingsutveksling.domain.ByteArrayFile;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;


public class CreateManifest {

	
	public no.difi.meldingsutveksling.dokumentpakking.domain.Manifest createManifest(Organisasjonsnummer avsenderOrg, Organisasjonsnummer mottakerOrg, ByteArrayFile hoveddokument){
		
		Avsender avsender = new Avsender(new Organisasjon(avsenderOrg));
		Mottaker mottaker = new Mottaker(new Organisasjon(mottakerOrg));
		HovedDokument hoveddokumentXml = new HovedDokument(hoveddokument.getFileName(), hoveddokument.getMimeType(), "Hoveddokument", "no");
		
		Manifest xmlManifest = new Manifest(mottaker, avsender, hoveddokumentXml);
		
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		MarshalManifest.marshal(xmlManifest, os);
		return new no.difi.meldingsutveksling.dokumentpakking.domain.Manifest(os.toByteArray());
	}
}
