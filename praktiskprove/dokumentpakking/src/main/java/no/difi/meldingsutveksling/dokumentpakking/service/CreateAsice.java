package no.difi.meldingsutveksling.dokumentpakking.service;

import java.util.ArrayList;
import java.util.List;

import no.difi.meldingsutveksling.dokumentpakking.crypto.CreateSignature;
import no.difi.meldingsutveksling.dokumentpakking.crypto.Signature;
import no.difi.meldingsutveksling.dokumentpakking.domain.Archive;
import no.difi.meldingsutveksling.dokumentpakking.domain.AsicEAttachable;
import no.difi.meldingsutveksling.dokumentpakking.domain.Avsender;

public class CreateAsice {
	private CreateSignature createSignature;
	private CreateZip createZip;

	public CreateAsice(CreateSignature createSignature, CreateZip createZip) {
		this.createSignature = createSignature;
		this.createZip = createZip;
	}

	public Archive createAsice(AsicEAttachable forsendelse, Avsender avsender) {
		List<AsicEAttachable> files = new ArrayList<AsicEAttachable>();
		files.add(forsendelse);

		Signature signature = createSignature.createSignature(avsender.getNoekkelpar(), files);
		files.add(signature);

		Archive archive = createZip.zipIt(files);
		
		return archive;
	}
}
