package no.difi.meldingsutveksling.dokumentpakking;

import java.io.ByteArrayOutputStream;

import no.difi.meldingsutveksling.dokumentpakking.crypto.CreateSignature;
import no.difi.meldingsutveksling.dokumentpakking.domain.AsicEAttachable;
import no.difi.meldingsutveksling.dokumentpakking.domain.Avsender;
import no.difi.meldingsutveksling.dokumentpakking.domain.Mottaker;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateAsice;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateSBD;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateZip;
import no.difi.meldingsutveksling.dokumentpakking.service.EncryptPayload;
import no.difi.meldingsutveksling.dokumentpakking.xml.MarshalSBD;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;

import org.unece.cefact.namespaces.standardbusinessdocumentheader.StandardBusinessDocument;

public class Dokumentpakker {

	private EncryptPayload encryptPayload;
	private CreateSBD createSBD;
	private CreateAsice createAsice;

	public Dokumentpakker(EncryptPayload encryptPayload,CreateAsice createAsice , CreateSBD createSBD) {
		this.createSBD = createSBD;
	}

	public Dokumentpakker() {
		createSBD = new CreateSBD();
		encryptPayload = new EncryptPayload();
		createAsice = new CreateAsice(new CreateSignature(), new CreateZip());
	}

	public byte[] pakkDokumentISbd(AsicEAttachable document, Avsender avsender, Mottaker mottaker) {
		Payload payload = new Payload(encryptPayload.encrypt(createAsice.createAsice(document, avsender).getBytes(), mottaker));
		StandardBusinessDocument doc = createSBD.createSBD(avsender.getOrgNummer(), mottaker.getOrgNummer(), payload);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		MarshalSBD.marshal(doc, os);
		return os.toByteArray();
	}
}
