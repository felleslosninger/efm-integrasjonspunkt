package no.difi.meldingsutveksling.dph.client.internal;

import no.difi.move.common.io.InMemoryWithTempFileFallbackResourceFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.codec.multipart.Part;
import reactor.core.publisher.Flux;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DphParcelServiceTest {

    @TempDir
    File temporaryFileDirectory;

    @Test
    void getEncryptedAsic_deletes_the_temporary_file_when_the_connection_is_lost_while_copying() {
        InMemoryWithTempFileFallbackResourceFactory resourceFactory = InMemoryWithTempFileFallbackResourceFactory.builder()
            .threshold(10)
            .initialBufferSize(10)
            .directory(temporaryFileDirectory)
            .build();
        DphParcelService target = new DphParcelService(null, null, null, null, null, resourceFactory);
        DataBuffer larger = DefaultDataBufferFactory.sharedInstance.wrap(new byte[1000]);
        Part part = mock(Part.class);
        when(part.content()).thenReturn(Flux.concat(Flux.just(larger), Flux.error(new IllegalStateException("Connection lost"))));

        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> target.getEncryptedAsic(part));

        assertThat(temporaryFileDirectory.listFiles()).isEmpty();
    }
}
