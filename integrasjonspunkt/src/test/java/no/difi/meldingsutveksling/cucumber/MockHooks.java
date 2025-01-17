package no.difi.meldingsutveksling.cucumber;

import io.cucumber.java.Before;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.UUIDGenerator;
import no.difi.meldingsutveksling.nextmove.InternalQueue;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveSender;
import org.mockito.stubbing.Answer;

import jakarta.persistence.EntityManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;

@RequiredArgsConstructor
public class MockHooks {

    private final UUIDGenerator uuidGenerator;
    private final InternalQueue internalQueue;
    private final NextMoveSender nextMoveSender;
    private final EntityManager entityManager;

    @Before
    @SneakyThrows
    public void before() {
        given(uuidGenerator.generate()).willReturn(
                "19efbd4c-413d-4e2c-bbc5-257ef4a65b38",
                "ac5efbd4c-413d-4e2c-bbc5-257ef4a65b23"
        );

        doAnswer((Answer<Void>) invocation -> {
            NextMoveOutMessage message = invocation.getArgument(0);
            entityManager.detach(message);
            nextMoveSender.send(message);
            return null;
        }).when(internalQueue).enqueueNextMove(any());
    }
}
