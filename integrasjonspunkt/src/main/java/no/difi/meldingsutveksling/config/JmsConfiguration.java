package no.difi.meldingsutveksling.config;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;

import javax.jms.ConnectionFactory;
import javax.jms.Session;

@Configuration
@EnableJms
@EnableConfigurationProperties(ActiveMQProperties.class)
public class JmsConfiguration {


    @Bean
    ConnectionFactory jmsConnectionFactory(ActiveMQProperties properties) {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
        RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
        redeliveryPolicy.setRedeliveryDelay(10000L);
        redeliveryPolicy.setMaximumRedeliveryDelay(1000*60*60);
        redeliveryPolicy.setInitialRedeliveryDelay(10000L);
        redeliveryPolicy.setBackOffMultiplier(3.0);
        redeliveryPolicy.setMaximumRedeliveries(30);
        redeliveryPolicy.setUseExponentialBackOff(true);
        connectionFactory.setRedeliveryPolicy(redeliveryPolicy);

        return connectionFactory;
    }

    @Bean
    DefaultJmsListenerContainerFactory myJmsContainerFactory(ActiveMQConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
        factory.setConnectionFactory(connectionFactory);
        return factory;
    }}
