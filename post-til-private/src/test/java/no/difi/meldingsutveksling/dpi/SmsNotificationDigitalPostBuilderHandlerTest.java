package no.difi.meldingsutveksling.dpi;

import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.sdp.client2.domain.digital_post.DigitalPost;
import no.difi.sdp.client2.domain.digital_post.SmsVarsel;
import org.junit.Before;
import org.junit.Test;

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
        SmsNotificationDigitalPostBuilderHandler handler = new SmsNotificationDigitalPostBuilderHandler(config);
        handler.handle(new Request().withNotifiable(true).withEmail("foo"), builder);

        verify(builder, never()).smsVarsel(any(SmsVarsel.class));
    }

    @Test
    public void notifiableFalseShouldNotSetSmsVarsel() {
        SmsNotificationDigitalPostBuilderHandler handler = new SmsNotificationDigitalPostBuilderHandler(config);
        handler.handle(new Request().withNotifiable(false), builder);

        verify(builder, never()).smsVarsel(any(SmsVarsel.class));
    }

    @Test
    public void notifiableAndSmsFeatureEnabledWithoutNumberShouldNotSetSmsVarsel() {
        SmsNotificationDigitalPostBuilderHandler handler = new SmsNotificationDigitalPostBuilderHandler(config);
        handler.handle(new Request().withNotifiable(true), builder);

        verify(builder, never()).smsVarsel(any(SmsVarsel.class));
    }

    @Test
    public void notifiableAndSmsFeatureEnabledWithNumberShouldSetSmsVarsel() {
        SmsNotificationDigitalPostBuilderHandler handler = new SmsNotificationDigitalPostBuilderHandler(config);
        handler.handle(new Request().withNotifiable(true).withMobileNumber("123").withSms("foo"), builder);

        verify(builder).smsVarsel(any(SmsVarsel.class));
    }
}