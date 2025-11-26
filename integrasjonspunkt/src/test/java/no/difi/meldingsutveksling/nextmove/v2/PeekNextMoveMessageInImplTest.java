package no.difi.meldingsutveksling.nextmove.v2;

import lombok.SneakyThrows;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.ArkivmeldingMessageAsAttachment;
import no.difi.meldingsutveksling.nextmove.NextMoveInMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = JacksonTestConfig.class)
class PeekNextMoveMessageInImplTest {

    @Autowired
    private Clock clock;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NextMoveMessageInRepository target;

    @AfterEach
    void afterTest() {
        target.deleteAll();
    }

    @Test
    @SneakyThrows
    void testFindIdsForUnlockedMessages() {
        NextMoveInMessage message1 = entityManager.persistAndFlush(getNextMoveMessage());
        TimeUnit.MILLISECONDS.sleep(1L); // Avoid that lastUpdated has identical values
        entityManager.persistAndFlush(getNextMoveMessage().setLockTimeout(OffsetDateTime.now(clock)));
        TimeUnit.MILLISECONDS.sleep(1L);
        NextMoveInMessage message3 = entityManager.persistAndFlush(getNextMoveMessage());

        assertThat(target.findIdsForUnlockedMessages(new NextMoveInMessageQueryInput(), 10))
                .containsExactlyInAnyOrder(message1.getId(), message3.getId());
    }

    @Test
    void testFindIdsForUnlockedMessagesWithConversationId() {
        NextMoveInMessage message1 = entityManager.persistAndFlush(getNextMoveMessage());

        assertThat(target.findIdsForUnlockedMessages(new NextMoveInMessageQueryInput().setConversationId("1"), 10))
                .isEmpty();

        message1.setConversationId("1");
        assertThat(target.findIdsForUnlockedMessages(new NextMoveInMessageQueryInput().setConversationId("1"), 10))
                .containsExactly(message1.getId());
    }

    @Test
    void testFindIdsForUnlockedMessagesWithMessageId() {
        NextMoveInMessage message1 = entityManager.persistAndFlush(getNextMoveMessage());

        assertThat(target.findIdsForUnlockedMessages(new NextMoveInMessageQueryInput().setMessageId("1"), 10))
                .isEmpty();

        message1.setMessageId("1");
        assertThat(target.findIdsForUnlockedMessages(new NextMoveInMessageQueryInput().setMessageId("1"), 10))
                .containsExactly(message1.getId());
    }

    @Test
    void testFindIdsForUnlockedMessagesWithReceiverIdentifier() {
        NextMoveInMessage message1 = entityManager.persistAndFlush(getNextMoveMessage());

        assertThat(target.findIdsForUnlockedMessages(new NextMoveInMessageQueryInput().setReceiverIdentifier("1"), 10))
                .isEmpty();

        message1.setReceiverIdentifier("1");
        assertThat(target.findIdsForUnlockedMessages(new NextMoveInMessageQueryInput().setReceiverIdentifier("1"), 10))
                .containsExactly(message1.getId());
    }

    @Test
    void testFindIdsForUnlockedMessagesWithSenderIdentifier() {
        NextMoveInMessage message1 = entityManager.persistAndFlush(getNextMoveMessage());

        assertThat(target.findIdsForUnlockedMessages(new NextMoveInMessageQueryInput().setSenderIdentifier("1"), 10))
                .isEmpty();

        message1.setSenderIdentifier("1");
        assertThat(target.findIdsForUnlockedMessages(new NextMoveInMessageQueryInput().setSenderIdentifier("1"), 10))
                .containsExactly(message1.getId());
    }

    @Test
    void testFindIdsForUnlockedMessagesWithsetServiceIdentifier() {
        NextMoveInMessage message1 = entityManager.persistAndFlush(getNextMoveMessage());

        assertThat(target.findIdsForUnlockedMessages(new NextMoveInMessageQueryInput().setServiceIdentifier("DPE"), 10))
                .isEmpty();

        message1.setServiceIdentifier(ServiceIdentifier.DPE);
        assertThat(target.findIdsForUnlockedMessages(new NextMoveInMessageQueryInput().setServiceIdentifier("DPE"), 10))
                .containsExactly(message1.getId());
    }

    @Test
    void testFindIdsForUnlockedMessagesWithsetProcessIdentifier() {
        NextMoveInMessage message1 = entityManager.persistAndFlush(getNextMoveMessage());

        assertThat(target.findIdsForUnlockedMessages(new NextMoveInMessageQueryInput().setProcess("1"), 10))
                .isEmpty();

        message1.setProcessIdentifier("1");
        assertThat(target.findIdsForUnlockedMessages(new NextMoveInMessageQueryInput().setProcess("1"), 10))
                .containsExactly(message1.getId());
    }

    @Test
    void testLock() {
        NextMoveInMessage message1 = entityManager.persist(getNextMoveMessage());
        NextMoveInMessage message2 = entityManager.persist(getNextMoveMessage().setLockTimeout(OffsetDateTime.now(clock)));

        OffsetDateTime lockTimeout = OffsetDateTime.now(clock).plusMinutes(10);

        assertThat(target.lock(message1.getId(), lockTimeout)).contains(message1);
        entityManager.refresh(message1);
        assertThat(message1.getLockTimeout()).isEqualTo(lockTimeout);

        assertThat(target.lock(message2.getId(), lockTimeout)).isEmpty();
    }

    private NextMoveInMessage getNextMoveMessage() {
        NextMoveInMessage message = new NextMoveInMessage();
        message.setSbd(new StandardBusinessDocument().setAny(new ArkivmeldingMessageAsAttachment()));
        return message;
    }
}
