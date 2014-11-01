package no.difi.meldingsutveksling.dokumentpakking.service;

import java.util.ArrayList;
import java.util.List;

import no.difi.meldingsutveksling.dokumentpakking.crypto.CreateSignature;
import no.difi.meldingsutveksling.dokumentpakking.crypto.Sertifikat;
import no.difi.meldingsutveksling.dokumentpakking.crypto.Signature;
import no.difi.meldingsutveksling.dokumentpakking.domain.Archive;
import no.difi.meldingsutveksling.dokumentpakking.domain.AsicEAttachable;
import no.difi.meldingsutveksling.dokumentpakking.domain.CMSDocument;
import no.difi.meldingsutveksling.dokumentpakking.domain.TekniskAvsender;

public class CreateCMScryptadedAsic {
	private CreateSignature createSignature;
	private CreateZip createZip;
	private CreateCMSDocument createCMS;

	public CreateCMScryptadedAsic(CreateSignature createSignature, CreateZip createZip, CreateCMSDocument createCMS) {
		this.createSignature = createSignature;
		this.createZip = createZip;
		this.createCMS = createCMS;
	}

	public CMSDocument createAsice(AsicEAttachable forsendelse, TekniskAvsender avsender, Sertifikat mottakersSertifikat) {
		List<AsicEAttachable> files = new ArrayList<AsicEAttachable>();
		files.add(forsendelse);

		Signature signature = createSignature.createSignature(avsender.getNoekkelpar(), files);
		files.add(signature);

		Archive archive = createZip.zipIt(files);
		CMSDocument cms = createCMS.createCMS(archive.getBytes(), mottakersSertifikat);
		return cms;
	}
}
