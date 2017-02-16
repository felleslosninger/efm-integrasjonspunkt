package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.apache.commons.lang.NotImplementedException;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StrategyFactoryTest {

    private StrategyFactory strategyFactory;

    @Before
    public void setup() {
        final MessageSender messageSender = mock(MessageSender.class);
        final IntegrasjonspunktProperties properties = mock(IntegrasjonspunktProperties.class);
        final IntegrasjonspunktProperties.PostVirksomheter ptvMock = mock(IntegrasjonspunktProperties.PostVirksomheter.class);
        final DigitalPostInnbyggerConfig dpic = mock(DigitalPostInnbyggerConfig.class);
        final DigitalPostInnbyggerConfig.Keystore keystore = mock(DigitalPostInnbyggerConfig.Keystore.class);
        when(dpic.getKeystore()).thenReturn(keystore);
        when(dpic.getFeature()).thenReturn(new DigitalPostInnbyggerConfig.FeatureToggle());
        when(messageSender.getProperties()).thenReturn(properties);
        when(properties.getAltinnPTV()).thenReturn(ptvMock);
        when(properties.getDpi()).thenReturn(dpic);
        final ServiceRegistryLookup serviceRegistryLookup = mock(ServiceRegistryLookup.class);

        final KeystoreProvider keystoreProvider = mock(KeystoreProvider.class);
        strategyFactory = new StrategyFactory(messageSender, serviceRegistryLookup, keystoreProvider, properties);
    }

    @Test
    public void givenEduServiceRecordShouldCreateEduMessageStrategyFactory() {
        // given
        ServiceRecord eduServiceRecord = new ServiceRecord("EDU", "12345678", "certificate", "http://localhost");

        // when
        final MessageStrategyFactory factory = strategyFactory.getFactory(eduServiceRecord);
        // then

        assertThat(factory, instanceOf(EduMessageStrategyFactory.class));
    }

    @Test
    public void givenPostServiceRecordShouldCreatePostMessageStrategyFactory() {
        ServiceRecord postServiceRecord = new ServiceRecord("POST_VIRKSOMHET", "12346442", "certificate", "http://localhost");

        final MessageStrategyFactory factory = strategyFactory.getFactory(postServiceRecord);

        assertThat(factory, instanceOf(PostVirksomhetStrategyFactory.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyServiceRecordThrowsException() {
        strategyFactory.getFactory(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void serviceRecordWithoutServiceIdentifierThrowsError() {
        strategyFactory.getFactory(new ServiceRecord(null, "1235465", "certificate", "http://localhost"));
    }

    @Test(expected = NotImplementedException.class)
    public void unknownServiceRecordThrowsException() {
        strategyFactory.getFactory(new ServiceRecord("unknown", "123456", "certificate", "http://localhost"));
    }

}
