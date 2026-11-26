package no.difi.meldingsutveksling.config;

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.activemq.autoconfigure.ActiveMQProperties;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.SimpleJmsListenerEndpoint;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.connection.SingleConnectionFactory;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Guards the connection factory wiring. A shared/cached connection factory under a listener container leaves
 * MessageConsumers registered on the broker after the container has released them, and reference counts
 * start()/stop() across every user of the single physical connection. Both leave outgoing NextMove messages
 * dispatched but never acknowledged, so they stay in status OPPRETTET. Caching is only allowed on the producer
 * side, and without consumer caching.
 */
class JmsConfigurationTest {

    private static final int CONCURRENCY = 10;

    private final JmsConfiguration jmsConfiguration = new JmsConfiguration();

    @Test
    void listenerContainerDoesNotUseASharedConnectionFactory() {
        DefaultMessageListenerContainer container = createListenerContainer();

        assertThat(container.getConnectionFactory()).isNotInstanceOf(SingleConnectionFactory.class);
    }

    @Test
    void listenerContainerUsesAFixedNumberOfConsumers() {
        DefaultMessageListenerContainer container = createListenerContainer();

        assertThat(container.getConcurrentConsumers()).isEqualTo(CONCURRENCY);
        assertThat(container.getMaxConcurrentConsumers()).isEqualTo(CONCURRENCY);
    }

    /**
     * A listener exception that escapes the error handler makes the container close and reopen the connection shared
     * by all consumers, and every message the other consumers are processing is redelivered - also those already sent.
     * The failed message itself is redelivered through Session.recover() regardless.
     */
    @Test
    void listenerExceptionsAreNotPropagatedToTheContainer() {
        DefaultMessageListenerContainer container = createListenerContainer();

        assertThat(container.getErrorHandler()).isNotNull();
        assertThatCode(() -> container.getErrorHandler().handleError(new RuntimeException("listener failed")))
                .doesNotThrowAnyException();
    }

    @Test
    void producerConnectionFactoryDoesNotCacheConsumers() {
        ConnectionFactory connectionFactory = jmsConfiguration.myJmsConnectionFactory(activeMqConnectionFactory());

        assertThat(connectionFactory).isInstanceOfSatisfying(CachingConnectionFactory.class,
                caching -> assertThat(caching.isCacheConsumers()).isFalse());
    }

    private DefaultMessageListenerContainer createListenerContainer() {
        DefaultJmsListenerContainerFactory factory =
                jmsConfiguration.myJmsContainerFactory(activeMqConnectionFactory(), integrasjonspunktProperties());

        SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
        endpoint.setId("test");
        endpoint.setDestination("nextmove");
        endpoint.setMessageListener(message -> {
        });

        return factory.createListenerContainer(endpoint);
    }

    private ActiveMQConnectionFactory activeMqConnectionFactory() {
        ActiveMQProperties activeMQProperties = new ActiveMQProperties();
        activeMQProperties.setBrokerUrl("tcp://localhost:61616");
        return jmsConfiguration.activeMqConnectionFactory(activeMQProperties, integrasjonspunktProperties());
    }

    private IntegrasjonspunktProperties integrasjonspunktProperties() {
        IntegrasjonspunktProperties.Queue queue = new IntegrasjonspunktProperties.Queue();
        queue.setConcurrency(CONCURRENCY);
        queue.setMaximumRetryHours(100);
        queue.setNextmoveName("nextmove");
        queue.setDlqName("ActiveMQ.DLQ");

        IntegrasjonspunktProperties properties = new IntegrasjonspunktProperties();
        properties.setQueue(queue);
        return properties;
    }
}
