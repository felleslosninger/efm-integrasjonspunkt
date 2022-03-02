package no.difi.meldingsutveksling.dpi.xmlsoap;

import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerRequest;
import no.difi.sdp.client2.domain.digital_post.DigitalPost;
import no.difi.sdp.client2.domain.digital_post.EpostVarsel;
import org.springframework.util.StringUtils;

import java.util.Arrays;

public class EmailNotificationDigitalPostBuilderHandler extends DigitalPostBuilderHandler {
    public EmailNotificationDigitalPostBuilderHandler(DigitalPostInnbyggerConfig config) {
        super(config);
    }

    @Override
    public DigitalPost.Builder handle(MeldingsformidlerRequest request, DigitalPost.Builder builder) {
        if (StringUtils.hasText(request.getEmailVarslingstekst()) &&
                request.isNotifiable() &&
                StringUtils.hasText(request.getEmailAddress())) {
            final EpostVarsel varsel = createVarselEttereForvaltningsforskriften(request);
            builder.epostVarsel(varsel);
        }
        return builder;
    }

    private EpostVarsel createVarselEttereForvaltningsforskriften(MeldingsformidlerRequest request) {
        return EpostVarsel.builder(request.getEmailAddress(), request.getEmailVarslingstekst())
                .varselEtterDager(Arrays.asList(0, 7))
                .build();
    }
}
