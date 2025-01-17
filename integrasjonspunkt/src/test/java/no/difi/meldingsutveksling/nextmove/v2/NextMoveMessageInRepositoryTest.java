package no.difi.meldingsutveksling.nextmove.v2;

import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.ArkivmeldingMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveInMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

@DataJpaTest
@Transactional(propagation = NOT_SUPPORTED) // we're going to handle transactions manually
@ActiveProfiles("test")
@Import({JacksonTestConfig.class})
class NextMoveMessageInRepositoryTest {

    @Autowired
    private Clock clock;

    @Autowired
    private NextMoveMessageInRepository target;

    @AfterEach
    void afterTest() {
        target.deleteAll();
    }

    @Test
    void testFindIdsForUnlockedMessages() {
        NextMoveInMessage message1 = target.save(getNextMoveMessage("M1", "C1"));
        NextMoveInMessage message2 = target.save(getNextMoveMessage("M2", "C1"));
        NextMoveInMessage message3 = target.save(getNextMoveMessage("M3", "C2"));

        assertThat(target.findIdsForUnlockedMessages(new NextMoveInMessageQueryInput()
                , 20)).containsOnly(message1.getId(), message2.getId(), message3.getId());

        assertThat(target.findIdsForUnlockedMessages(new NextMoveInMessageQueryInput()
                        .setMessageId("M1")
                        .setConversationId("C1")
                , 20)).containsOnly(message1.getId());

        assertThat(target.findIdsForUnlockedMessages(new NextMoveInMessageQueryInput()
                        .setMessageId("M2")
                        .setConversationId("C2")
                , 20)).isEmpty();

        assertThat(target.findIdsForUnlockedMessages(new NextMoveInMessageQueryInput()
                        .setConversationId("C1")
                , 20)).containsOnly(message1.getId(), message2.getId());

        assertThat(target.findIdsForUnlockedMessages(new NextMoveInMessageQueryInput()
                        .setMessageId("M3")
                , 20)).containsOnly(message3.getId());
    }

    @Test
    void testLock() {
        NextMoveInMessage message1 = target.save(getNextMoveMessage("M1", "C1"));
        NextMoveInMessage message2 = target.save(getNextMoveMessage("M2", "C1"));
        NextMoveInMessage message3 = target.save(getNextMoveMessage("M3", "C2"));

        assertThat(target.lock(message1.getId(), OffsetDateTime.now().plusMinutes(10))).contains(message1);
        assertThat(target.lock(message2.getId(), OffsetDateTime.now().plusMinutes(10))).contains(message2);
        assertThat(target.lock(message3.getId(), OffsetDateTime.now().plusMinutes(10))).contains(message3);
    }

    @Test
    void testMultiThreadedPeek() {
        Set<NextMoveInMessage> messages = IntStream.range(0, 100)
                .mapToObj(p -> target.save(getNextMoveMessage()))
                .collect(Collectors.toSet());

        IntStream.range(0, 100)
                .parallel()
                .mapToObj(this::peekLock)
                .flatMap(Optional::stream)
                .forEach(message -> assertThat(messages.remove(message)).isTrue());

        assertThat(messages.size()).isLessThan(50);
    }

    private Optional<NextMoveInMessage> peekLock(int i) {
        OffsetDateTime lockTimeout = OffsetDateTime.now(clock).plusMinutes(10);

        for (Long id : target.findIdsForUnlockedMessages(new NextMoveInMessageQueryInput(), 20)) {
            Optional<NextMoveInMessage> lockedMessage = target.lock(id, lockTimeout);
            if (lockedMessage.isPresent()) {
                return lockedMessage;
            }
        }

        return Optional.empty();
    }

    private NextMoveInMessage getNextMoveMessage(String messageId, String conversationId) {
        NextMoveInMessage message = getNextMoveMessage();
        message.setMessageId(messageId);
        message.setConversationId(conversationId);
        return message;
    }

    private NextMoveInMessage getNextMoveMessage() {
        NextMoveInMessage message = new NextMoveInMessage();
        message.setSbd(new StandardBusinessDocument().setAny(new ArkivmeldingMessage()));
        return message;
    }
}