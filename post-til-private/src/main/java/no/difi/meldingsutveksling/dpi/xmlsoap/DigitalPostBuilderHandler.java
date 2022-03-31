package no.difi.meldingsutveksling.dpi.xmlsoap;

import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerRequest;
import no.difi.sdp.client2.domain.digital_post.DigitalPost;

public abstract class DigitalPostBuilderHandler {
    private final DigitalPostInnbyggerConfig config;

    protected DigitalPostBuilderHandler(DigitalPostInnbyggerConfig config) {
        this.config = config;
    }

    public abstract DigitalPost.Builder handle(MeldingsformidlerRequest request, DigitalPost.Builder builder);

    public DigitalPostInnbyggerConfig getConfig() {
        return config;
    }
}
