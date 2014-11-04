package no.difi.meldingsutveksling.dokumentpakking;

import java.io.ByteArrayOutputStream;

import no.difi.meldingsutveksling.dokumentpakking.crypto.CreateSignature;
import no.difi.meldingsutveksling.dokumentpakking.domain.AsicEAttachable;
import no.difi.meldingsutveksling.dokumentpakking.domain.Avsender;
import no.difi.meldingsutveksling.dokumentpakking.domain.Mottaker;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateCMSDocument;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateCMScryptadedAsic;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateSBD;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateZip;
import no.difi.meldingsutveksling.dokumentpakking.xml.MarshalSBD;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;

import org.unece.cefact.namespaces.standardbusinessdocumentheader.StandardBusinessDocument;

public class Dokumentpakker {

	private CreateCMScryptadedAsic createCMScryptadedAsic;
	private CreateSBD createSBD;

	public Dokumentpakker(CreateCMScryptadedAsic createCMScryptadedAsic, CreateSBD createSBD) {
		this.createCMScryptadedAsic = createCMScryptadedAsic;
		this.createSBD = createSBD;
	}

	public Dokumentpakker() {
		this.createCMScryptadedAsic = new CreateCMScryptadedAsic(new CreateSignature(), new CreateZip(), new CreateCMSDocument());
		createSBD = new CreateSBD();
	}

	public byte[] pakkDokumentISbd(AsicEAttachable document, Avsender avsender, Mottaker mottaker) {
		Payload payload = new Payload(createCMScryptadedAsic.createAsice(document, avsender, mottaker).getBytes(), "UTF-8", "texkt/xml");
		StandardBusinessDocument doc = createSBD.createSBD(avsender.getOrgNummer(), mottaker.getOrgNummer(), payload);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		MarshalSBD.marshal(doc, os);
		return os.toByteArray();
	}
}
