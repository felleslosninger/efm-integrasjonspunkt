package no.difi.meldingsutveksling.dpi.forsendelse;

import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.meldingsutveksling.dpi.ForsendelseBuilderHandler;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerRequest;
import no.difi.sdp.client2.domain.AktoerOrganisasjonsnummer;
import no.difi.sdp.client2.domain.Avsender;
import no.difi.sdp.client2.domain.Dokumentpakke;
import no.difi.sdp.client2.domain.Forsendelse;
import no.difi.sdp.client2.domain.Sertifikat;
import no.difi.sdp.client2.domain.TekniskMottaker;
import no.difi.sdp.client2.domain.fysisk_post.FysiskPost;
import no.difi.sdp.client2.domain.fysisk_post.KonvoluttAdresse;
import no.difi.sdp.client2.domain.fysisk_post.Posttype;
import no.difi.sdp.client2.domain.fysisk_post.Returhaandtering;
import no.difi.sdp.client2.domain.fysisk_post.Utskriftsfarge;
import no.digipost.api.representations.Organisasjonsnummer;

public class PrintForsendelseHandler extends ForsendelseBuilderHandler {
    public PrintForsendelseHandler(DigitalPostInnbyggerConfig config) {
        super(config);
    }

    @Override
    public Forsendelse.Builder handle(MeldingsformidlerRequest request, Dokumentpakke dokumentpakke) {
        final AktoerOrganisasjonsnummer aktoerOrganisasjonsnummer = AktoerOrganisasjonsnummer.of(request.getSenderOrgnumber());
        Avsender avsender = Avsender.builder(aktoerOrganisasjonsnummer.forfremTilAvsender()).build();


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

        KonvoluttAdresse kon = konvoluttAdresseHandler.handle(request);
        // TODO property for utskriftsfarge, return strategy (shredding?), property for posttype dvs. a_prioritet eller b_økonomi
        TekniskMottaker utskriftsleverandoer = new TekniskMottaker(Organisasjonsnummer.of(request.getOrgnrPostkasse()), Sertifikat.fraByteArray(request.getCertificate()));
        FysiskPost fysiskPost = FysiskPost.builder().adresse(kon)
                .retur(Returhaandtering.MAKULERING_MED_MELDING,
                        returAdresseHandler.handle(request))
                .sendesMed(Posttype.A_PRIORITERT).utskrift(Utskriftsfarge.SORT_HVIT, utskriftsleverandoer).build();

        return Forsendelse.fysisk(avsender, fysiskPost, dokumentpakke);
    }
}
