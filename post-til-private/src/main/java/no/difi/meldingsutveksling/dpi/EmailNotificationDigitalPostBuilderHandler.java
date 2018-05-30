package no.difi.meldingsutveksling.dpi;

import com.google.common.collect.Lists;
import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.sdp.client2.domain.digital_post.DigitalPost;
import no.difi.sdp.client2.domain.digital_post.EpostVarsel;

import static com.google.common.base.Strings.isNullOrEmpty;

public class EmailNotificationDigitalPostBuilderHandler extends DigitalPostBuilderHandler {
    public EmailNotificationDigitalPostBuilderHandler(DigitalPostInnbyggerConfig config) {
        super(config);
    }

    @Override
    public DigitalPost.Builder handle(MeldingsformidlerRequest request, DigitalPost.Builder builder) {
        if (getConfig().isEnableEmailNotification() &&
                request.isNotifiable() &&
                !isNullOrEmpty(request.getEmailAddress())) {
            final EpostVarsel varsel = createVarselEttereForvaltningsforskriften(request);
            builder.epostVarsel(varsel);
        }
        return builder;
    }

    private EpostVarsel createVarselEttereForvaltningsforskriften(MeldingsformidlerRequest request) {
        return EpostVarsel.builder(request.getEmailAddress(), request.getEmailVarslingstekst())
                        .varselEtterDager(Lists.newArrayList(0, 7))
                        .build();
    }
}
