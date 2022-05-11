package no.difi.meldingsutveksling.dpi.xmlsoap.forsendelse;

import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerRequest;
import no.difi.meldingsutveksling.dpi.xmlsoap.ForsendelseBuilderHandler;
import no.difi.sdp.client2.domain.*;
import no.difi.sdp.client2.domain.fysisk_post.FysiskPost;
import no.difi.sdp.client2.domain.fysisk_post.KonvoluttAdresse;
import no.digipost.api.representations.Organisasjonsnummer;

import java.util.*;

public class PrintForsendelseHandler extends ForsendelseBuilderHandler {
    private static final Set<String> NORWAY_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("NORGE", "NORWAY", "NO", "NOR")));

    public PrintForsendelseHandler(DigitalPostInnbyggerConfig config) {
        super(config);
    }

    @Override
    public Forsendelse.Builder handle(MeldingsformidlerRequest request, Dokumentpakke dokumentpakke) {
        final AktoerOrganisasjonsnummer aktoerOrganisasjonsnummer = AktoerOrganisasjonsnummer.of(
                Optional.ofNullable(request.getOnBehalfOf())
                        .orElse(request.getSender())
                        .getOrganizationIdentifier()
        );
        Avsender.Builder avsenderBuilder = Avsender.builder(aktoerOrganisasjonsnummer.forfremTilAvsender());
        Optional.ofNullable(request.getAvsenderIdentifikator()).ifPresent(avsenderBuilder::avsenderIdentifikator);
        Optional.ofNullable(request.getFakturaReferanse()).ifPresent(avsenderBuilder::fakturaReferanse);
        Avsender avsender = avsenderBuilder.build();

        KonvoluttAdresseHandler konvoluttAdresseHandler;
        if (isNorge(request.getPostAddress().getLand())) {
            konvoluttAdresseHandler = new NorgeKonvoluttAdresseHandler();
        } else {
            konvoluttAdresseHandler = new UtlandKonvoluttAdresseHandler();
        }
        KonvoluttAdresseHandler returAdresseHandler;
        if (isNorge(request.getReturnAddress().getLand())) {
            returAdresseHandler = new NorgeKonvoluttAdresseHandler();
        } else {
            returAdresseHandler = new UtlandKonvoluttAdresseHandler();
        }

        KonvoluttAdresse kon = konvoluttAdresseHandler.handle(request.getPostAddress());
        TekniskMottaker utskriftsleverandoer = new TekniskMottaker(
                Organisasjonsnummer.of(request.getPostkasseProvider().getOrganizationIdentifier()),
                Sertifikat.fraByteArray(request.getCertificate()));

        FysiskPost fysiskPost = FysiskPost.builder()
                .adresse(kon)
                .retur(SDPEnumUtil.getReturhaandtering(request.getReturnHandling()),
                        returAdresseHandler.handle(request.getReturnAddress()))
                .sendesMed(SDPEnumUtil.getPosttype(request.getPostalCategory()))
                .utskrift(SDPEnumUtil.getUtskriftsfarge(request.getPrintColor()), utskriftsleverandoer)
                .build();

        return Forsendelse.fysisk(avsender, fysiskPost, dokumentpakke);
    }

    private boolean isNorge(String land) {
        return (land == null || "".equals(land)) || NORWAY_SET.contains(land.toUpperCase());
    }

}
