package no.difi.meldingsutveksling.dpi.forsendelse;

import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.meldingsutveksling.dpi.EmailNotificationDigitalPostBuilderHandler;
import no.difi.meldingsutveksling.dpi.ForsendelseBuilderHandler;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerRequest;
import no.difi.meldingsutveksling.dpi.SmsNotificationDigitalPostBuilderHandler;
import no.difi.sdp.client2.domain.*;
import no.difi.sdp.client2.domain.digital_post.DigitalPost;
import no.digipost.api.representations.Organisasjonsnummer;

public class DigitalForsendelseHandler extends ForsendelseBuilderHandler {
    private final SmsNotificationDigitalPostBuilderHandler smsNotificationHandler;
    private final EmailNotificationDigitalPostBuilderHandler emailNotificationHandler;

    public DigitalForsendelseHandler(DigitalPostInnbyggerConfig config) {
        super(config);
        smsNotificationHandler = new SmsNotificationDigitalPostBuilderHandler(config);
        emailNotificationHandler = new EmailNotificationDigitalPostBuilderHandler(config);
    }

    @Override
    public Forsendelse.Builder handle(MeldingsformidlerRequest request, Dokumentpakke dokumentpakke) {
        Mottaker mottaker = Mottaker.builder(
                request.getMottakerPid(),
                request.getPostkasseAdresse(),
                Sertifikat.fraByteArray(request.getCertificate()),
                Organisasjonsnummer.of(request.getOrgnrPostkasse())
        ).build();

        final AktoerOrganisasjonsnummer aktoerOrganisasjonsnummer = AktoerOrganisasjonsnummer.of(request.getOnBehalfOfOrgnr().orElse(request.getSenderOrgnumber()));
        DigitalPost.Builder digitalPost = DigitalPost.builder(mottaker, request.getSubject())
                .virkningsdato(request.getVirkningsdato())
                .aapningskvittering(request.isAapningskvittering())
                .sikkerhetsnivaa(request.getSecurityLevel());
        digitalPost = smsNotificationHandler.handle(request, digitalPost);
        digitalPost = emailNotificationHandler.handle(request, digitalPost);
        Avsender.Builder avsenderBuilder = Avsender.builder(aktoerOrganisasjonsnummer.forfremTilAvsender());
        request.getAvsenderIdentifikator().ifPresent(avsenderBuilder::avsenderIdentifikator);
        Avsender behandlingsansvarlig = avsenderBuilder.build();
        return Forsendelse.digital(behandlingsansvarlig, digitalPost.build(), dokumentpakke);
    }
}
