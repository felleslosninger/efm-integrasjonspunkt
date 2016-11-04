package no.difi.meldingsutveksling.ptp;

import no.difi.sdp.client2.domain.digital_post.DigitalPost;
import no.difi.sdp.client2.domain.digital_post.EpostVarsel;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class SmsNotificationDigitalPostBuilderHandlerTest {

    private MeldingsformidlerClient.Config config;
    private DigitalPost.Builder builder;

    @Before
    public void setup() {
        config = mock(MeldingsformidlerClient.Config.class);
        builder = mock(DigitalPost.Builder.class);
    }

    @Test
    public void smsNotificationDisabledShouldNotSetEpostVarsel() {
        when(config.isEnableSms()).thenReturn(false);

        SmsNotificationDigitalPostBuilderHandler handler = new SmsNotificationDigitalPostBuilderHandler(config);
        handler.handle(new Request().withNotifiable(true), builder);

        verify(builder, never()).epostVarsel(any(EpostVarsel.class));
    }

    @Test
    public void notifiableFalseShouldNotSetEpostVarsel() {
        when(config.isEnableSms()).thenReturn(true);

        SmsNotificationDigitalPostBuilderHandler handler = new SmsNotificationDigitalPostBuilderHandler(config);
        handler.handle(new Request().withNotifiable(false), builder);

        verify(builder, never()).epostVarsel(any(EpostVarsel.class));
    }

    @Test
    public void notifiableAndSmsFeatureEnabledShouldSetEpostVarsel() {
        when(config.isEnableSms()).thenReturn(true);

        SmsNotificationDigitalPostBuilderHandler handler = new SmsNotificationDigitalPostBuilderHandler(config);
        handler.handle(new Request().withNotifiable(true), builder);

        verify(builder).epostVarsel(any(EpostVarsel.class));
    }

}