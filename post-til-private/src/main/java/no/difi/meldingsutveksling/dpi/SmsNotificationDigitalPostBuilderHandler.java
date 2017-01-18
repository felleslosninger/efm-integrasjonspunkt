package no.difi.meldingsutveksling.dpi;

import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.sdp.client2.domain.digital_post.DigitalPost;
import no.difi.sdp.client2.domain.digital_post.SmsVarsel;

public class SmsNotificationDigitalPostBuilderHandler extends DigitalPostBuilderHandler {
    public SmsNotificationDigitalPostBuilderHandler(DigitalPostInnbyggerConfig config) {
        super(config);
    }

    @Override
    public DigitalPost.Builder handle(MeldingsformidlerRequest request, DigitalPost.Builder builder) {
        if (getConfig().isEnableSmsNotification() && request.isNotifiable()) {
            builder.smsVarsel(SmsVarsel.builder(request.getMobileNumber(), request.getSmsVarslingstekst()).build());
        }
        return builder;
    }
}
