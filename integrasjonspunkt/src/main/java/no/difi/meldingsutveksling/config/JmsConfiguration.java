package no.difi.meldingsutveksling.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.util.StringUtils;

import javax.jms.ConnectionFactory;
import javax.jms.Session;

@Slf4j
@Configuration
@EnableJms
@EnableConfigurationProperties({ActiveMQProperties.class, IntegrasjonspunktProperties.class})
public class JmsConfiguration {


    @Bean
    ConnectionFactory jmsConnectionFactory(ActiveMQProperties activeMQProps, IntegrasjonspunktProperties props) {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(activeMQProps.getBrokerUrl());
        if (StringUtils.hasText(activeMQProps.getUser())) {
            connectionFactory.setUserName(activeMQProps.getUser());
        }
        if (StringUtils.hasText(activeMQProps.getPassword())) {
            connectionFactory.setPassword(activeMQProps.getPassword());
        }

        RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
        redeliveryPolicy.setRedeliveryDelay(20000L);
        redeliveryPolicy.setMaximumRedeliveryDelay(1000L * 60L * 60L);
        redeliveryPolicy.setInitialRedeliveryDelay(20000L);
        redeliveryPolicy.setBackOffMultiplier(3.0d);
        // 5 retries will happen within the first hour. After that, get max retries from properties (default 20).
        redeliveryPolicy.setMaximumRedeliveries(5 + props.getQueue().getMaximumRetryHours());
        redeliveryPolicy.setUseExponentialBackOff(true);
        connectionFactory.setRedeliveryPolicy(redeliveryPolicy);
        connectionFactory.setNonBlockingRedelivery(true);

        connectionFactory.setUseAsyncSend(true);

        return new CachingConnectionFactory(connectionFactory);
    }

    @Bean
    DefaultJmsListenerContainerFactory myJmsContainerFactory(ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
        factory.setConnectionFactory(connectionFactory);
        factory.setErrorHandler(t -> log.warn("JMS error: {}", t.getMessage()));
        return factory;
    }
}
