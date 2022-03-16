package no.difi.meldingsutveksling.cucumber;

import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.MimeTypeExtensionMapper;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.dokumentpakking.domain.Document;
import no.ks.fiks.io.client.FiksIOKlient;
import no.ks.fiks.io.client.model.*;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@RequiredArgsConstructor
public class FiksIoOutSteps {

    private final Holder<Message> messageSentHolder;
    private final FiksIOKlient fiksIOKlient;

    @Before
    public void before() {
        messageSentHolder.reset();

        SendtMelding sendtMelding = SendtMelding.builder()
                .meldingId(new MeldingId(UUID.randomUUID()))
                .avsenderKontoId(new KontoId(UUID.randomUUID()))
                .mottakerKontoId(new KontoId(UUID.randomUUID()))
                .meldingType("")
                .ttl(Duration.ofHours(1))
                .build();
        doReturn(sendtMelding)
                .when(fiksIOKlient)
                .send(any(), anyList());

    }

    @Then("^a message is sent to FIKS IO with kontoId \"([^\"]+)\" and protocol \"([^\"]+)\"$")
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public void aMessageIsSentToFiksIo(String kontoId, String protocol) {
        ArgumentCaptor<MeldingRequest> captor = ArgumentCaptor.forClass(MeldingRequest.class);
        ArgumentCaptor<List<Payload>> payloadCaptor = ArgumentCaptor.forClass(List.class);
        verify(fiksIOKlient).send(captor.capture(), payloadCaptor.capture());

        MeldingRequest request = captor.getValue();
        assertEquals(kontoId, request.getMottakerKontoId().toString());
        assertEquals(protocol, request.getMeldingType());

        List<Payload> payloads = payloadCaptor.getValue();
        List<Document> attachments = new ArrayList<>();
        for (Payload p : payloads) {
            try (InputStream inputStream = p.getPayload()) {
                attachments.add(Document.builder()
                        .resource(new ByteArrayResource(StreamUtils.copyToByteArray(inputStream)))
                        .filename(p.getFilnavn())
                        .mimeType(MimeTypeExtensionMapper.getMimetype(Stream.of(p.getFilnavn().split("\\.")).reduce((a, b) -> b).orElse("pdf")))
                        .build());
            }
        }

        Message message = new Message()
                .setAttachments(attachments)
                .setServiceIdentifier(ServiceIdentifier.DPFIO);
        messageSentHolder.set(message);
    }
}
