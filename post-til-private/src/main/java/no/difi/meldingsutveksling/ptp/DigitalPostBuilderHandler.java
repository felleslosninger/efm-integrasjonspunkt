package no.difi.meldingsutveksling.ptp;

import no.difi.sdp.client2.domain.digital_post.DigitalPost;

public abstract class DigitalPostBuilderHandler {
    private final MeldingsformidlerClient.Config config;

    public DigitalPostBuilderHandler(MeldingsformidlerClient.Config config) {
        this.config = config;
    }

    public abstract DigitalPost.Builder handle(MeldingsformidlerRequest request, DigitalPost.Builder builder);

    public MeldingsformidlerClient.Config getConfig() {
        return config;
    }
}
