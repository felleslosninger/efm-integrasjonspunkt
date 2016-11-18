package no.difi.meldingsutveksling.dpi;

import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.sdp.client2.domain.digital_post.DigitalPost;
import no.difi.sdp.client2.domain.digital_post.EpostVarsel;

public class EmailNotificationDigitalPostBuilderHandler extends DigitalPostBuilderHandler {
    public EmailNotificationDigitalPostBuilderHandler(DigitalPostInnbyggerConfig config) {
        super(config);
    }

    @Override
    public DigitalPost.Builder handle(MeldingsformidlerRequest request, DigitalPost.Builder builder) {
        if (getConfig().isEnableEmailNotification() && request.isNotifiable()) {
            builder.epostVarsel(EpostVarsel.builder(request.getEmail(), request.getVarslingstekst()).build());
        }
        return builder;
    }
}
