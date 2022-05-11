package no.difi.meldingsutveksling.nextmove.message;


import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class BugFix610Test {

    private static byte[] OVERLAY = new byte[]{80, 75, 3, 4, 10, 0, 0, 8, 0, 0, -43, 104, 103, 76, -118, 33};

    @Test
    public void testApplyPatch() {
        byte[] message = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
        BugFix610.applyPatch(message, "id");
        for (int i = 0; i < OVERLAY.length; ++i) {
            assertThat(message[i]).isEqualTo(OVERLAY[i]);
        }

        assertThat(message[16]).isEqualTo((byte) 16);
        assertThat(message[17]).isEqualTo((byte) 17);
        assertThat(message[18]).isEqualTo((byte) 18);
        assertThat(message[19]).isEqualTo((byte) 19);
    }

    @Test
    public void testApplyPatchStreamed() throws IOException {
        byte[] message = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19};

        InputStream inputStream = BugFix610.applyPatch(new ByteArrayInputStream(message), "id");

        byte[] buffer = new byte[20];

        assertThat(inputStream.read(buffer)).isEqualTo(20);

        for (int i = 0; i < OVERLAY.length; ++i) {
            assertThat(buffer[i]).isEqualTo(OVERLAY[i]);
        }

        assertThat(buffer[16]).isEqualTo((byte) 16);
        assertThat(buffer[17]).isEqualTo((byte) 17);
        assertThat(buffer[18]).isEqualTo((byte) 18);
        assertThat(buffer[19]).isEqualTo((byte) 19);
    }
}