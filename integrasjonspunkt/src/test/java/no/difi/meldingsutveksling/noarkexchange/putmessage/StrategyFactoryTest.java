package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

public class StrategyFactoryTest {

    private StrategyFactory strategyFactory;

    @Before
    public void setup() {
        strategyFactory = new StrategyFactory(new MessageSender());
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
}