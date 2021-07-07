package no.difi.meldingsutveksling.dpi.xmlsoap.forsendelse;

import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerRequest;
import no.difi.meldingsutveksling.dpi.xmlsoap.ForsendelseBuilderHandler;
import no.difi.sdp.client2.domain.*;
import no.difi.sdp.client2.domain.fysisk_post.FysiskPost;
import no.difi.sdp.client2.domain.fysisk_post.KonvoluttAdresse;
import no.digipost.api.representations.Organisasjonsnummer;

public class PrintForsendelseHandler extends ForsendelseBuilderHandler {
    public PrintForsendelseHandler(DigitalPostInnbyggerConfig config) {
        super(config);
    }

    @Override
    public Forsendelse.Builder handle(MeldingsformidlerRequest request, Dokumentpakke dokumentpakke) {
        final AktoerOrganisasjonsnummer aktoerOrganisasjonsnummer = AktoerOrganisasjonsnummer.of(request.getOnBehalfOfOrgnr().orElse(request.getSenderOrgnumber()));
        Avsender.Builder avsenderBuilder = Avsender.builder(aktoerOrganisasjonsnummer.forfremTilAvsender());
        request.getAvsenderIdentifikator().ifPresent(avsenderBuilder::avsenderIdentifikator);
        request.getFakturaReferanse().ifPresent(avsenderBuilder::fakturaReferanse);
        Avsender avsender = avsenderBuilder.build();

        KonvoluttAdresseHandler konvoluttAdresseHandler;
        if (request.getPostAddress().isNorge()) {
            konvoluttAdresseHandler = new NorgeKonvoluttAdresseHandler();
        } else {
            konvoluttAdresseHandler = new UtlandKonvoluttAdresseHandler();
        }
        KonvoluttAdresseHandler returAdresseHandler;
        if (request.getReturnAddress().isNorge()) {
            returAdresseHandler = new NorgeKonvoluttAdresseHandler();
        } else {
            returAdresseHandler = new UtlandKonvoluttAdresseHandler();
        }

        KonvoluttAdresse kon = konvoluttAdresseHandler.handle(request.getPostAddress());
        TekniskMottaker utskriftsleverandoer = new TekniskMottaker(Organisasjonsnummer.of(request.getOrgnrPostkasse()), Sertifikat.fraByteArray(request.getCertificate()));

        FysiskPost fysiskPost = FysiskPost.builder()
                .adresse(kon)
                .retur(SDPEnumUtil.getReturhaandtering(request.getReturnHandling()),
                        returAdresseHandler.handle(request.getReturnAddress()))
                .sendesMed(SDPEnumUtil.getPosttype(request.getPostalCategory()))
                .utskrift(SDPEnumUtil.getUtskriftsfarge(request.getPrintColor()), utskriftsleverandoer)
                .build();

        return Forsendelse.fysisk(avsender, fysiskPost, dokumentpakke);
    }
}
