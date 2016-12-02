package no.difi.meldingsutveksling.dpi.forsendelse;

import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.meldingsutveksling.dpi.EmailNotificationDigitalPostBuilderHandler;
import no.difi.meldingsutveksling.dpi.ForsendelseBuilderHandler;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerRequest;
import no.difi.meldingsutveksling.dpi.SmsNotificationDigitalPostBuilderHandler;
import no.difi.sdp.client2.domain.AktoerOrganisasjonsnummer;
import no.difi.sdp.client2.domain.Avsender;
import no.difi.sdp.client2.domain.Dokumentpakke;
import no.difi.sdp.client2.domain.Forsendelse;
import no.difi.sdp.client2.domain.Mottaker;
import no.difi.sdp.client2.domain.Sertifikat;
import no.difi.sdp.client2.domain.digital_post.DigitalPost;
import no.digipost.api.representations.Organisasjonsnummer;

import java.util.Date;

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

        final AktoerOrganisasjonsnummer aktoerOrganisasjonsnummer = AktoerOrganisasjonsnummer.of(request.getSenderOrgnumber());
        DigitalPost.Builder digitalPost = DigitalPost.builder(mottaker, request.getSubject())
                .virkningsdato(new Date())
                .sikkerhetsnivaa(config.getSecurityLevel());
        digitalPost = smsNotificationHandler.handle(request, digitalPost);
        digitalPost = emailNotificationHandler.handle(request, digitalPost);
        Avsender behandlingsansvarlig = Avsender.builder(aktoerOrganisasjonsnummer.forfremTilAvsender()).build();
        return Forsendelse.digital(behandlingsansvarlig, digitalPost.build(), dokumentpakke);
    }
}
