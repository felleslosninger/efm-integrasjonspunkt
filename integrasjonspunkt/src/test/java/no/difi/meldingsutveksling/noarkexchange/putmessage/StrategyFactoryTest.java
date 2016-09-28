package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.apache.commons.lang.NotImplementedException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.env.Environment;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StrategyFactoryTest {

    private StrategyFactory strategyFactory;

    @Before
    public void setup() {
        final MessageSender messageSender = mock(MessageSender.class);
        final Environment environment = mock(Environment.class);
        when(messageSender.getEnvironment()).thenReturn(environment);
        final ServiceRegistryLookup serviceRegistryLookup = mock(ServiceRegistryLookup.class);

        strategyFactory = new StrategyFactory(messageSender, serviceRegistryLookup);
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