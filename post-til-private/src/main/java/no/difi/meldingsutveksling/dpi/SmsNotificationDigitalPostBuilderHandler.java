package no.difi.meldingsutveksling.dpi;

import com.google.common.collect.Lists;
import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.sdp.client2.domain.digital_post.DigitalPost;
import no.difi.sdp.client2.domain.digital_post.SmsVarsel;

import static com.google.common.base.Strings.isNullOrEmpty;

public class SmsNotificationDigitalPostBuilderHandler extends DigitalPostBuilderHandler {
    public SmsNotificationDigitalPostBuilderHandler(DigitalPostInnbyggerConfig config) {
        super(config);
    }

    @Override
    public DigitalPost.Builder handle(MeldingsformidlerRequest request, DigitalPost.Builder builder) {
        if (getConfig().isEnableSmsNotification() &&
                request.isNotifiable() &&
                !isNullOrEmpty(request.getMobileNumber())) {
            final SmsVarsel varsel = createVarselEttereForvaltningsforskriften(request);
            builder.smsVarsel(varsel);
        }
        return builder;
    }

    private SmsVarsel createVarselEttereForvaltningsforskriften(MeldingsformidlerRequest request) {
        return SmsVarsel.builder(request.getMobileNumber(), request.getSmsVarslingstekst())
                .varselEtterDager(Lists.newArrayList(0, 7))
                .build();
    }
}
