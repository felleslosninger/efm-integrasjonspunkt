package no.difi.meldingsutveksling.dokumentpakking.service;

import no.difi.meldingsutveksling.dokumentpakking.crypto.CreateSignature;
import no.difi.meldingsutveksling.dokumentpakking.domain.Archive;
import no.difi.meldingsutveksling.dokumentpakking.domain.AsicEAttachable;
import no.difi.meldingsutveksling.dokumentpakking.domain.Avsender;
import no.difi.meldingsutveksling.dokumentpakking.domain.CMSDocument;
import no.difi.meldingsutveksling.dokumentpakking.domain.Mottaker;

public class CreateCMScryptadedAsic {
	private CreateCMSDocument createCMS;
	private CreateAsice createAsic;

	public CreateCMScryptadedAsic(CreateSignature createSignature, CreateZip createZip, CreateCMSDocument createCMS) {
		this.createCMS = createCMS;
		this.createAsic = new CreateAsice(createSignature, createZip);
	}

	public CMSDocument createAsice(AsicEAttachable forsendelse, Avsender avsender, Mottaker mottaker) {
		Archive archive = createAsic.createAsice(forsendelse, avsender);
		CMSDocument cms = createCMS.createCMS(archive.getBytes(), mottaker.getSertifikat());
		return cms;
	}
}
