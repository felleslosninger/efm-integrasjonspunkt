package no.difi.meldingsutveksling.dpi.xmlsoap.forsendelse;

import no.difi.begrep.sdp.schema_v10.SDPSikkerhetsnivaa;
import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerRequest;
import no.difi.meldingsutveksling.dpi.xmlsoap.EmailNotificationDigitalPostBuilderHandler;
import no.difi.meldingsutveksling.dpi.xmlsoap.ForsendelseBuilderHandler;
import no.difi.meldingsutveksling.dpi.xmlsoap.SmsNotificationDigitalPostBuilderHandler;
import no.difi.sdp.client2.domain.*;
import no.difi.sdp.client2.domain.digital_post.DigitalPost;
import no.difi.sdp.client2.domain.digital_post.Sikkerhetsnivaa;
import no.digipost.api.representations.Organisasjonsnummer;

import java.util.Optional;

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
                request.getMottakerPid().getIdentifier(),
                request.getPostkasseAdresse(),
                Sertifikat.fraByteArray(request.getCertificate()),
                Organisasjonsnummer.of(request.getPostkasseProvider().getOrganizationIdentifier())
        ).build();

        final AktoerOrganisasjonsnummer aktoerOrganisasjonsnummer = AktoerOrganisasjonsnummer.of(
                Optional.ofNullable(request.getOnBehalfOf())
                        .orElse(request.getSender())
                        .getOrganizationIdentifier()
        );
        DigitalPost.Builder digitalPost = DigitalPost.builder(mottaker, request.getSubject())
                .virkningsdato(java.util.Date.from(request.getVirkningsdato().toInstant()))
                .aapningskvittering(request.isAapningskvittering())
                .sikkerhetsnivaa(getSikkerhetsnivaa(request));
        digitalPost = smsNotificationHandler.handle(request, digitalPost);
        digitalPost = emailNotificationHandler.handle(request, digitalPost);
        Avsender.Builder avsenderBuilder = Avsender.builder(aktoerOrganisasjonsnummer.forfremTilAvsender());
        Optional.ofNullable(request.getAvsenderIdentifikator()).ifPresent(avsenderBuilder::avsenderIdentifikator);
        Optional.ofNullable(request.getFakturaReferanse()).ifPresent(avsenderBuilder::fakturaReferanse);
        Avsender behandlingsansvarlig = avsenderBuilder.build();
        return Forsendelse.digital(behandlingsansvarlig, digitalPost.build(), dokumentpakke);
    }

    private Sikkerhetsnivaa getSikkerhetsnivaa(MeldingsformidlerRequest request) {
        Integer securityLevel = request.getSecurityLevel();
        if (securityLevel == null) {
            return null;
        }
        return Sikkerhetsnivaa.valueOf(SDPSikkerhetsnivaa.fromValue(securityLevel.toString()).toString());
    }
}
