package no.difi.meldingsutveksling.ptp;

import no.difi.sdp.client2.domain.digital_post.DigitalPost;
import no.difi.sdp.client2.domain.digital_post.EpostVarsel;

public class SmsNotificationDigitalPostBuilderHandler extends DigitalPostBuilderHandler {
    public SmsNotificationDigitalPostBuilderHandler(DigitalPostInnbyggerConfig config) {
        super(config);
    }

    @Override
    public DigitalPost.Builder handle(MeldingsformidlerRequest request, DigitalPost.Builder builder) {
        if (getConfig().isEnableSmsNotification() && request.isNotifiable()) {
            builder.epostVarsel(EpostVarsel.builder(request.getEmail(), request.getVarslingstekst()).build());
        }
        return builder;
    }
}
