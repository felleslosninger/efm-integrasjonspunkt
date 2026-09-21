package no.difi.meldingsutveksling.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQPrefetchPolicy;
import org.apache.activemq.RedeliveryPolicy;
import org.springframework.boot.activemq.autoconfigure.ActiveMQProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.scheduling.support.TaskUtils;
import org.springframework.util.StringUtils;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Session;

@Slf4j
@Configuration
@EnableJms
@EnableConfigurationProperties({ActiveMQProperties.class, IntegrasjonspunktProperties.class})
public class JmsConfiguration {

    /**
     * The raw, uncached connection factory. This is the factory the listener containers use; it is also
     * the target of the caching factory used on the producer side.
     */
    @Bean
    ActiveMQConnectionFactory activeMqConnectionFactory(ActiveMQProperties activeMQProps, IntegrasjonspunktProperties props) {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(activeMQProps.getBrokerUrl());
        if (StringUtils.hasText(activeMQProps.getUser())) {
            connectionFactory.setUserName(activeMQProps.getUser());
        }
        if (StringUtils.hasText(activeMQProps.getPassword())) {
            connectionFactory.setPassword(activeMQProps.getPassword());
        }

        connectionFactory.setRedeliveryPolicy(getRedeliveryPolicy(props));
        connectionFactory.setNonBlockingRedelivery(true);
        connectionFactory.setUseAsyncSend(true);
        connectionFactory.setPrefetchPolicy(getActiveMQPrefetchPolicy());

        return connectionFactory;
    }

    /**
     * Caching is for the producer side ({@link org.springframework.jms.core.JmsTemplate}) only. It must not be
     * handed to a listener container: {@code CachingConnectionFactory} keeps a {@code MessageConsumer} registered
     * on the broker after the listener container has released it, and it reference counts {@code start()} /
     * {@code stop()} across every user of the single shared connection. Either one leaves messages dispatched to
     * a consumer that nobody polls - they are never acknowledged and never rolled back, so outgoing NextMove
     * messages stay in status OPPRETTET until their time to live expires. Consumer caching is disabled here as
     * well, so that a stray injection of this bean into a listener container cannot reintroduce the first problem.
     */
    @Bean
    @Primary
    ConnectionFactory myJmsConnectionFactory(ActiveMQConnectionFactory activeMqConnectionFactory) {
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(activeMqConnectionFactory);
        cachingConnectionFactory.setCacheConsumers(false);
        return cachingConnectionFactory;
    }

    private ActiveMQPrefetchPolicy getActiveMQPrefetchPolicy() {
        ActiveMQPrefetchPolicy prefetchPolicy = new ActiveMQPrefetchPolicy();
        prefetchPolicy.setQueuePrefetch(0);
        prefetchPolicy.setTopicPrefetch(0);
        return prefetchPolicy;
    }

    private RedeliveryPolicy getRedeliveryPolicy(IntegrasjonspunktProperties props) {
        RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
        redeliveryPolicy.setRedeliveryDelay(20000L);
        redeliveryPolicy.setMaximumRedeliveryDelay(1000L * 60L * 60L);
        redeliveryPolicy.setInitialRedeliveryDelay(20000L);
        redeliveryPolicy.setBackOffMultiplier(3.0d);
        // 5 retries will happen within the first hour. After that, get max retries from properties (default 20).
        redeliveryPolicy.setMaximumRedeliveries(5 + props.getQueue().getMaximumRetryHours());
        redeliveryPolicy.setUseExponentialBackOff(true);
        return redeliveryPolicy;
    }

    /**
     * Also registered under the name {@code jmsListenerContainerFactory} so that listeners without an explicit
     * {@code containerFactory} - the dead letter queue listener in {@code InternalQueue} - get this factory
     * instead of the auto configured one, which would be built on the caching connection factory.
     */
    @Bean(name = {"jmsListenerContainerFactory", "myJmsContainerFactory"})
    DefaultJmsListenerContainerFactory myJmsContainerFactory(ActiveMQConnectionFactory activeMqConnectionFactory, IntegrasjonspunktProperties props) {
        int consumers = Math.max(1, props.getQueue().getConcurrency());

        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
        factory.setConnectionFactory(activeMqConnectionFactory);
        factory.setErrorHandler(TaskUtils.getDefaultErrorHandler(false));
        // A bare number means "1 to N" - dynamic scaling, which Spring explicitly warns against combining with
        // connection factory caching. Keep the number of consumers fixed.
        factory.setConcurrency(consumers + "-" + consumers);
        return factory;
    }
}
