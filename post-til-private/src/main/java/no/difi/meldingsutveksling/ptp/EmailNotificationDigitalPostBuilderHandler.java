package no.difi.meldingsutveksling.ptp;

import no.difi.sdp.client2.domain.digital_post.DigitalPost;
import no.difi.sdp.client2.domain.digital_post.EpostVarsel;

public class EmailNotificationDigitalPostBuilderHandler extends DigitalPostBuilderHandler {
    public EmailNotificationDigitalPostBuilderHandler(MeldingsformidlerClient.Config config) {
        super(config);
    }

    @Override
    public DigitalPost.Builder handle(MeldingsformidlerRequest request, DigitalPost.Builder builder) {
        if (getConfig().isEnableEmail() && request.isNotifiable()) {
            builder.epostVarsel(EpostVarsel.builder(request.getEmail(), request.getVarslingstekst()).build());
        }
        return builder;
    }
}
