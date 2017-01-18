package no.difi.meldingsutveksling.dpi;

import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.sdp.client2.domain.digital_post.DigitalPost;
import no.difi.sdp.client2.domain.digital_post.SmsVarsel;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class SmsNotificationDigitalPostBuilderHandlerTest {

    private DigitalPostInnbyggerConfig config;
    private DigitalPost.Builder builder;

    @Before
    public void setup() {
        config = mock(DigitalPostInnbyggerConfig.class);
        builder = mock(DigitalPost.Builder.class);
    }

    @Test
    public void smsNotificationDisabledShouldNotSetEpostVarsel() {
        when(config.isEnableSmsNotification()).thenReturn(false);

        SmsNotificationDigitalPostBuilderHandler handler = new SmsNotificationDigitalPostBuilderHandler(config);
        handler.handle(new Request().withNotifiable(true), builder);

        verify(builder, never()).smsVarsel(any(SmsVarsel.class));
    }

    @Test
    public void notifiableFalseShouldNotSetSmsVarsel() {
        when(config.isEnableEmailNotification()).thenReturn(true);

        SmsNotificationDigitalPostBuilderHandler handler = new SmsNotificationDigitalPostBuilderHandler(config);
        handler.handle(new Request().withNotifiable(false), builder);

        verify(builder, never()).smsVarsel(any(SmsVarsel.class));
    }

    @Test
    public void notifiableAndSmsFeatureEnabledShouldSetSmsVarsel() {
        when(config.isEnableSmsNotification()).thenReturn(true);

        SmsNotificationDigitalPostBuilderHandler handler = new SmsNotificationDigitalPostBuilderHandler(config);
        handler.handle(new Request().withNotifiable(true), builder);

        verify(builder).smsVarsel(any(SmsVarsel.class));
    }
}