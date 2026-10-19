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

    // TODO denne fiksen er antagelig ikke aktuell lenger, mulig det var eFormidlig 1.0 og et
    // behov for å hotfix av noen Altinn ZIP filer.  Men lar koden ligge inntil videre, siden
    // den uansett er deaktivert som default.
    //
    // Fiksen enables med : difi.move.nextmove.apply-zip-header-patch=true
    // Informasjon av hva/hvorfor er "tynn" : https://digdir.atlassian.net/browse/MOVE-610
    //
    // Hardkodingen over i hex : 50 4B 03 04 0A 00 00 08 00 00 D5 68 67 4C 8A 21
    //
    // Det ser ut som en standard ZIP header med følgende informasjon hardkodet :
    //
    // 50 4B 03 04   Local file header signature: PK\x03\x04
    // 0A 00         Version needed to extract: 10 = ZIP spec 1.0
    // 00 08         General purpose bit flag: 0x0800 = UTF-8 filename/comment encoding
    // 00 00         Compression method: 0 = stored, no compression
    // D5 68         Last modified time: DOS time 13:06:42
    // 67 4C         Last modified date: DOS date 2018-03-07

}
