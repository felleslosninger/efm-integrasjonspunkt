package no.difi.meldingsutveksling.nextmove.v2;

import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.ArkivmeldingMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveInMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
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
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = JacksonTestConfig.class)
class PeekMultiThreadedTest {

    @Autowired
    private Clock clock;

    @Autowired
    private NextMoveMessageInRepository target;

    @AfterEach
    void afterTest() {
        target.deleteAll();
    }

    @Test
    void testMultiThreadedPeek() {
        Set<NextMoveInMessage> messages = IntStream.range(0, 100)
                .mapToObj(p -> target.save(getNextMoveMessage()))
                .collect(Collectors.toSet());

        IntStream.range(0, 100)
                .parallel()
                .mapToObj(this::peekLock)
                .filter(Optional::isPresent)
                .map(Optional::get)
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

    private NextMoveInMessage getNextMoveMessage() {
        NextMoveInMessage message = new NextMoveInMessage();
        message.setSbd(new StandardBusinessDocument().setAny(new ArkivmeldingMessage()));
        return message;
    }
}