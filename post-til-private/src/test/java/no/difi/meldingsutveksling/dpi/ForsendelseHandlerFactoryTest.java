package no.difi.meldingsutveksling.dpi;

import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.meldingsutveksling.dpi.forsendelse.DigitalForsendelseHandler;
import no.difi.meldingsutveksling.dpi.forsendelse.PrintForsendelseHandler;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ForsendelseHandlerFactoryTest {

    private DigitalPostInnbyggerConfig config;
    private MeldingsformidlerRequest mock;
    private ForsendelseHandlerFactory factory;

    @Before
    public void setup() {
        config = new DigitalPostInnbyggerConfig();
        mock = mock(MeldingsformidlerRequest.class);
        factory = new ForsendelseHandlerFactory(config);
    }

    @Test
    public void shouldCreateDigitalForsendelseHandler() {
        when(mock.isPrintProvider()).thenReturn(false);

        ForsendelseBuilderHandler forsendelseBuilderHandler = factory.create(mock);

        assertThat(forsendelseBuilderHandler, instanceOf(DigitalForsendelseHandler.class));
    }

    @Test
    public void shouldCreatePrintForsendelseHandler() {
        when(mock.isPrintProvider()).thenReturn(true);

        ForsendelseBuilderHandler forsendelseBuilderHandler = factory.create(mock);

        assertThat(forsendelseBuilderHandler, instanceOf(PrintForsendelseHandler.class));
    }
}