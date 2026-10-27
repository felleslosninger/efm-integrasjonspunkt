package no.difi.meldingsutveksling.webhooks;

import no.difi.meldingsutveksling.webhooks.event.MessageStatusContent;
import no.difi.meldingsutveksling.webhooks.event.WebhookEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// @EnableAsync is deliberately left out, so the listener runs synchronously and can be verified directly
@SpringJUnitConfig(WebhookEventListenerTest.Config.class)
class WebhookEventListenerTest {

    @Configuration
    @EnableTransactionManagement
    @Import(WebhookEventListener.class)
    static class Config {
        @Bean
        PlatformTransactionManager transactionManager() {
            return new NoOpTransactionManager();
        }
    }

    @MockitoBean
    private WebhookPusher webhookPusher;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private WebhookEvent<MessageStatusContent> event;

    @BeforeEach
    void setUp() {
        event = new WebhookEvent<>(mock(MessageStatusContent.class), this);
    }

    @Test
    void pushesWithoutTransaction() {
        publisher.publishEvent(event);

        verify(webhookPusher).push(event);
    }

    @Test
    void pushesOnlyAfterCommit() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            publisher.publishEvent(event);
            verify(webhookPusher, never()).push(any());
        });

        verify(webhookPusher).push(event);
    }

    @Test
    void doesNotPushOnRollback() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            publisher.publishEvent(event);
            status.setRollbackOnly();
        });

        verify(webhookPusher, never()).push(any());
    }

    static class NoOpTransactionManager extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
