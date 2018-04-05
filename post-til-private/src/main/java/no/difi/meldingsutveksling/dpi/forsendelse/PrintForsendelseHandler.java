package no.difi.meldingsutveksling.dpi.forsendelse;

import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.meldingsutveksling.config.dpi.PrintSettings;
import no.difi.meldingsutveksling.dpi.ForsendelseBuilderHandler;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerRequest;
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
        TekniskMottaker utskriftsleverandoer = new TekniskMottaker(Organisasjonsnummer.of(request.getOrgnrPostkasse()), Sertifikat.fraByteArray(request.getCertificate()));
        final PrintSettings printSettings = request.getPrintSettings();
        FysiskPost fysiskPost = FysiskPost.builder().adresse(kon)
                .retur(printSettings.getReturnType().toExternal(),
                        returAdresseHandler.handle(request))
                .sendesMed(printSettings.getShippingType().toExternal()).utskrift(printSettings.getInkType().toExternal(), utskriftsleverandoer).build();

        return Forsendelse.fysisk(avsender, fysiskPost, dokumentpakke);
    }
}
