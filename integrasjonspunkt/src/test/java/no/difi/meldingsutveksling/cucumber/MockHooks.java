package no.difi.meldingsutveksling.cucumber;

import cucumber.api.java.Before;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.UUIDGenerator;
import no.difi.meldingsutveksling.dpi.SikkerDigitalPostKlientFactory;
import no.difi.meldingsutveksling.nextmove.NextMoveSender;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.sdp.client2.SikkerDigitalPostKlient;
import no.difi.vefa.peppol.common.model.DocumentTypeIdentifier;
import no.difi.vefa.peppol.lookup.LookupClient;
import org.mockito.stubbing.Answer;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;

@RequiredArgsConstructor
public class MockHooks {

    private final UUIDGenerator uuidGenerator;
    private final LookupClient lookupClient;
    private final InternalQueue internalQueue;
    private final NextMoveSender nextMoveSender;
    private final SikkerDigitalPostKlientFactory sikkerDigitalPostKlientFactory;
    private final SikkerDigitalPostKlient sikkerDigitalPostKlient;

    @Before
    @SneakyThrows
    public void before() {
        given(uuidGenerator.generate()).willReturn(
                "19efbd4c-413d-4e2c-bbc5-257ef4a65b38",
                "ac5efbd4c-413d-4e2c-bbc5-257ef4a65b23"
        );

        given(lookupClient.getDocumentIdentifiers(any()))
                .willReturn(Collections.singletonList(DocumentTypeIdentifier.of("urn:no:difi:meldingsutveksling:2.0")));

        doAnswer((Answer<Void>) invocation -> {
            nextMoveSender.send(invocation.getArgument(0));
            return null;
        }).when(internalQueue).enqueueNextMove2(any());

        given(sikkerDigitalPostKlientFactory.createSikkerDigitalPostKlient(any())).willReturn(sikkerDigitalPostKlient);
        given(sikkerDigitalPostKlientFactory.createSikkerDigitalPostKlient(any(), any())).willReturn(sikkerDigitalPostKlient);
    }
}
