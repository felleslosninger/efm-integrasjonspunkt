package no.difi.meldingsutveksling.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQPrefetchPolicy;
import org.apache.activemq.RedeliveryPolicy;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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


    @Bean
    ConnectionFactory myJmsConnectionFactory(ActiveMQProperties activeMQProps, IntegrasjonspunktProperties props) {
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

        return new CachingConnectionFactory(connectionFactory);
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

    @Bean
    DefaultJmsListenerContainerFactory myJmsContainerFactory(ConnectionFactory myJmsConnectionFactory, IntegrasjonspunktProperties props) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
        factory.setConnectionFactory(myJmsConnectionFactory);
        factory.setErrorHandler(TaskUtils.getDefaultErrorHandler(false));
        factory.setConcurrency(String.valueOf(props.getQueue().getConcurrency()));
        return factory;
    }
}
