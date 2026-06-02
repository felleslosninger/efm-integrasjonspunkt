package no.difi.meldingsutveksling.nextmove;

import no.difi.meldingsutveksling.ks.svarinn.Forsendelse;
import no.difi.meldingsutveksling.ks.svarinn.SvarInnService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DefaultDpfPollingTest {
    @InjectMocks
    private DefaultDpfPolling target;

    @Mock
    private SvarInnService svarInnService;
    @Mock
    private SvarInnNextMoveForwarder svarInnNextMoveForwarder;

    @Test
    public void poll_processes_all_messages_even_though_one_throws_error(){
        Forsendelse message1 = new Forsendelse().setId("message1");
        Forsendelse message2 = new Forsendelse().setId("message2");
        Forsendelse message3 = new Forsendelse().setId("message3");

        when(svarInnService.getForsendelser()).thenReturn(List.of(message1, message2, message3));
        lenient().doThrow(new RuntimeException("Test exception"))
            .when(svarInnNextMoveForwarder)
            .accept(message1);

        assertDoesNotThrow(
            () -> target.poll(),
            "poll() should continue processing and not throw when first message fails"
        );

        InOrder inOrder = inOrder(svarInnNextMoveForwarder);

        assertAll("message1 must be processed first and fail; message2 and message3 must still be processed " +
                "afterward. Otherwise this test does not validate continue-on-error behavior",
            () -> inOrder.verify(svarInnNextMoveForwarder).accept(message1),
            () -> inOrder.verify(svarInnNextMoveForwarder).accept(message2),
            () -> inOrder.verify(svarInnNextMoveForwarder).accept(message3),
            () -> verifyNoMoreInteractions(svarInnNextMoveForwarder)
        );
    }
}
