package no.difi.meldingsutveksling.status;

import io.vavr.collection.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import java.time.OffsetDateTime;

import static org.junit.Assert.assertTrue;

@DataJpaTest
class MessageStatusRepositoryTest {

    @Autowired
    private MessageStatusRepository messageStatusRepository;

    @Test
    void testFindMessageStatusInDateRange() {
        OffsetDateTime fromDateTime = OffsetDateTime.parse("2023-06-09T10:00:00.600+02:00");
        OffsetDateTime toDateTime = OffsetDateTime.parse("2023-06-10T10:00:00.600+02:00");

        MessageStatus message1 = new MessageStatus("MOTTATT", fromDateTime.minusMinutes(30), "Message 1");
        MessageStatus message2 = new MessageStatus("LEVERT", fromDateTime.plusMinutes(15), "Message 2");
        MessageStatus message3 = new MessageStatus("OPPRETTET", toDateTime.minusMinutes(15), "Message 3");
        MessageStatus message4 = new MessageStatus("LEVERT", toDateTime.plusMinutes(30), "Message 4");

        messageStatusRepository.saveAll(List.of(message1, message2, message3, message4));

        MessageStatusQueryInput queryInput = new MessageStatusQueryInput();
        queryInput.setFromDateTime(fromDateTime);
        queryInput.setToDateTime(toDateTime);

        Page<MessageStatus> result = messageStatusRepository.find(queryInput, PageRequest.of(0, 10));

        for (MessageStatus messageStatus : result) {
            OffsetDateTime lastUpdate = messageStatus.getLastUpdate();
            assertTrue(lastUpdate.isAfter(fromDateTime) || lastUpdate.isEqual(fromDateTime));
            assertTrue(lastUpdate.isBefore(toDateTime) || lastUpdate.isEqual(toDateTime));
        }
    }
}
