package no.difi.meldingsutveksling.dokumentpakking.service;

import java.util.ArrayList;
import java.util.List;

import no.difi.meldingsutveksling.dokumentpakking.crypto.CreateSignature;
import no.difi.meldingsutveksling.dokumentpakking.crypto.Signature;
import no.difi.meldingsutveksling.dokumentpakking.domain.Archive;
import no.difi.meldingsutveksling.dokumentpakking.domain.AsicEAttachable;
import no.difi.meldingsutveksling.dokumentpakking.domain.Avsender;
import no.difi.meldingsutveksling.dokumentpakking.domain.Mottaker;

public class CreateAsice {
	private CreateSignature createSignature;
	private CreateZip createZip;
	private CreateManifest createManifest;

	public CreateAsice(CreateSignature createSignature, CreateZip createZip, CreateManifest createManifest) {
		this.createSignature = createSignature;
		this.createZip = createZip;
		this.createManifest = createManifest;
	}

	public Archive createAsice(AsicEAttachable forsendelse, Avsender avsender, Mottaker mottaker) {
		List<AsicEAttachable> files = new ArrayList<AsicEAttachable>();
		files.add(forsendelse);
		files.add(createManifest.createManifest(avsender.getOrgNummer(), mottaker.getOrgNummer(), forsendelse));

		Signature signature = createSignature.createSignature(avsender.getNoekkelpar(), files);
		files.add(signature);
		
		Archive archive = createZip.zipIt(files);
		
		return archive;
	}
}
