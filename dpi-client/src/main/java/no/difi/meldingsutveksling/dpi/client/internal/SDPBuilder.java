package no.difi.meldingsutveksling.dpi.client.internal;

import no.difi.meldingsutveksling.dpi.client.domain.Document;
import no.difi.meldingsutveksling.dpi.client.domain.Shipment;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.PersonmottakerHolder;
import no.difi.meldingsutveksling.dpi.client.domain.sbd.Avsender;
import no.difi.meldingsutveksling.dpi.client.domain.sbd.Identifikator;
import no.difi.meldingsutveksling.dpi.client.sdp.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SDPBuilder {

    public SDPManifest createManifest(Shipment shipment) {
        return SDPManifest.builder()
                .withMottaker(getMottaker(shipment))
                .withAvsender(getAvsender(shipment))
                .withHoveddokument(getHoveddokument(shipment))
                .withVedleggs(getVedlegg(shipment))
                .build();
    }

    private SDPAvsender getAvsender(Shipment shipment) {
        return Optional.ofNullable(shipment.getBusinessMessage())
                .map(message -> getAvsender(message.getAvsender()))
                .orElse(null);
    }

    private SDPDokument getHoveddokument(Shipment shipment) {
        return sdpDokument(shipment.getParcel().getMainDocument(), shipment.getLanguage());
    }

    private List<SDPDokument> getVedlegg(Shipment shipment) {
        return shipment.getParcel().getAttachments()
                .stream()
                .map(document -> sdpDokument(document, shipment.getLanguage()))
                .collect(Collectors.toList());
    }

    private SDPDokument sdpDokument(final Document document, final String spraakkode) {
        return SDPDokument.builder()
                .withTittel(getTittel(document, spraakkode))
                .withData(getDokumentData(document))
                .withHref(document.getFilename())
                .withMime(document.getMimeType())
                .build();
    }

    private SDPDokumentData getDokumentData(Document document) {
        return Optional.ofNullable(document.getMetadataDocument())
                .map(d -> SDPDokumentData.builder()
                        .withHref(d.getFilename())
                        .withMime(d.getMimeType())
                        .build())
                .orElse(null);
    }

    private SDPTittel getTittel(Document document, String spraakkode) {
        return document.getTitle() != null ? SDPTittel.builder()
                .withValue(document.getTitle())
                .withLang(spraakkode)
                .build() : null;
    }

    private SDPMottaker getMottaker(Shipment shipment) {
        return shipment.getBusinessMessage(PersonmottakerHolder.class)
                .map(PersonmottakerHolder::getMottaker)
                .map(mottaker ->
                        SDPMottaker.builder()
                                .withPerson(SDPPerson.builder()
                                        .withPostkasseadresse(mottaker.getPostkasseadresse())
                                        .build()).build())
                .orElse(null);
    }

    private SDPAvsender getAvsender(Avsender avsender) {
        return SDPAvsender.builder()
                .withOrganisasjon(getOrganisasjon(avsender.getVirksomhetsidentifikator()))
                .withAvsenderidentifikator(avsender.getAvsenderidentifikator())
                .withFakturaReferanse(avsender.getFakturaReferanse())
                .build();
    }

    private SDPOrganisasjon getOrganisasjon(Identifikator identifikator) {
        return SDPOrganisasjon.builder()
                .withValue(identifikator.getValue())
                .withAuthority(SDPIso6523Authority.fromValue(identifikator.getAuthority()))
                .build();
    }

}
