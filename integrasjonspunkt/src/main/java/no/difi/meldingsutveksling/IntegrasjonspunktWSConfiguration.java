package no.difi.meldingsutveksling;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.jms.ConnectionFactory;
import javax.jms.Session;


/**
 * @author Dervis M, 13/08/15.
 */

@Configuration
@ComponentScan("no.difi")
@ImportResource({"classpath*:rest-servlet.xml"})
@EnableJms
@EnableConfigurationProperties(ActiveMQProperties.class)
public class IntegrasjonspunktWSConfiguration{

    @Bean
    ConnectionFactory jmsConnectionFactory(ActiveMQProperties properties) {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");

        RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
     //   redeliveryPolicy.setMaximumRedeliveries(2);
        redeliveryPolicy.setMaximumRedeliveryDelay(5000L);
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
    }

}
