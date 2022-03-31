package no.difi.meldingsutveksling.dpi.xmlsoap;

import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerRequest;
import no.difi.sdp.client2.domain.digital_post.DigitalPost;
import no.difi.sdp.client2.domain.digital_post.SmsVarsel;
import org.springframework.util.StringUtils;

import java.util.Arrays;

public class SmsNotificationDigitalPostBuilderHandler extends DigitalPostBuilderHandler {
    public SmsNotificationDigitalPostBuilderHandler(DigitalPostInnbyggerConfig config) {
        super(config);
    }

    @Override
    public DigitalPost.Builder handle(MeldingsformidlerRequest request, DigitalPost.Builder builder) {
        if (StringUtils.hasText(request.getSmsVarslingstekst()) &&
                request.isNotifiable() &&
                StringUtils.hasText(request.getMobileNumber())) {
            final SmsVarsel varsel = createVarselEttereForvaltningsforskriften(request);
            builder.smsVarsel(varsel);
        }
        return builder;
    }

    private SmsVarsel createVarselEttereForvaltningsforskriften(MeldingsformidlerRequest request) {
        return SmsVarsel.builder(request.getMobileNumber(), request.getSmsVarslingstekst())
                .varselEtterDager(Arrays.asList(0, 7))
                .build();
    }
}
