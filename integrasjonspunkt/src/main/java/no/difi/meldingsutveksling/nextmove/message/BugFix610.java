package no.difi.meldingsutveksling.nextmove.message;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

@Slf4j
@UtilityClass
public class BugFix610 {

    public static boolean applyPatch(byte[] message, String messageId) {

        if (message.length >= 16) {
            applyPatchBytes(message, messageId);

            return true;
        }

        log.debug("Unable to applying patch 610 to incomming message with messageId: {}. Message to short", messageId);

        return false;
    }

    public static Resource applyPatch(InputStreamSource input, String messageId) throws IOException {
        return new InputStreamResource(applyPatch(input.getInputStream(), messageId));
    }

    public static InputStream applyPatch(InputStream inputStream, String messageId) throws IOException {
        PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream, 16);

        byte[] buffer = new byte[16];

        if (pushbackInputStream.read(buffer) != 16) {
            log.debug("Unable to applying patch 610 to incomming message with messageId: {}. Message to short", messageId);
            return inputStream;
        }

        applyPatchBytes(buffer, messageId);
        pushbackInputStream.unread(buffer);
        return pushbackInputStream;
    }

    private static void applyPatchBytes(byte[] message, String messageId) {
        log.debug("Applying patch 610 to incomming message with messageId: {}", messageId);

        message[0] = 80;
        message[1] = 75;
        message[2] = 3;
        message[3] = 4;

        message[4] = 10;
        message[5] = 0;
        message[6] = 0;
        message[7] = 8;

        message[8] = 0;
        message[9] = 0;
        message[10] = -43;
        message[11] = 104;

        message[12] = 103;
        message[13] = 76;
        message[14] = -118;
        message[15] = 33;
    }
}
