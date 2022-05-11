package no.difi.meldingsutveksling.nextmove.message;


import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;


public class AutoClosingInputStreamTest {

    private boolean callbackCalled;

    @Test
    public void testRead() throws IOException {
        AutoClosingInputStream is = new AutoClosingInputStream(new ByteArrayInputStream("Yo".getBytes(StandardCharsets.UTF_8)), () -> {
        });

        assertThat(is.read()).isEqualTo((int) 'Y');
        assertThat(is.isClosed()).isFalse();
        assertThat(is.read()).isEqualTo((int) 'o');
        assertThat(is.isClosed()).isTrue();
        assertThat(is.read()).isEqualTo(-1);
        assertThat(is.isClosed()).isTrue();
    }

    @Test
    public void testReadBytes() throws IOException {
        AutoClosingInputStream is = new AutoClosingInputStream(new ByteArrayInputStream("Hello".getBytes(StandardCharsets.UTF_8)), () -> {
        });

        byte[] buffer = new byte[3];
        assertThat(is.read(buffer)).isEqualTo(3);
        assertThat(is.isClosed()).isFalse();
        assertThat(is.read(buffer)).isEqualTo(2);
        assertThat(is.isClosed()).isTrue();
        assertThat(is.read(buffer)).isEqualTo(-1);
    }

    @Test
    public void testReadBytesWithOffsetAndLength() throws IOException {
        AutoClosingInputStream is = new AutoClosingInputStream(new ByteArrayInputStream("Hello".getBytes(StandardCharsets.UTF_8)), () -> {
        });

        byte[] buffer = new byte[5];
        assertThat(is.read(buffer, 1, 3)).isEqualTo(3);
        assertThat(is.isClosed()).isFalse();
        assertThat(is.read(buffer, 1, 3)).isEqualTo(2);
        assertThat(is.isClosed()).isTrue();
        assertThat(is.read(buffer, 1, 3)).isEqualTo(-1);
    }

    @Test
    public void testCallback() throws IOException {
        callbackCalled = false;

        AutoClosingInputStream is = new AutoClosingInputStream(new ByteArrayInputStream("Hello".getBytes(StandardCharsets.UTF_8)), () -> {
            callbackCalled = true;
        });

        assertThat(callbackCalled).isFalse();
        byte[] buffer = new byte[5];
        assertThat(is.read(buffer)).isEqualTo(5);
        assertThat(callbackCalled).isTrue();
    }
}