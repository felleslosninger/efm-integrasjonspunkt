package no.difi.meldingsutveksling.nextmove.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BugFix610 {

    private static final Logger LOGGER = LoggerFactory.getLogger(BugFix610.class);

    private BugFix610(){}

    /** Bug fix for #MOVE-610 */
    public static boolean applyPatch(byte[] message, String conversationId){

        if(message.length >= 16){
            LOGGER.debug("Applying patch 610 to incomming message with conversationId: {}", conversationId);

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

            return true;
        }

        LOGGER.debug("Unable to applying patch 610 to incomming message with conversationId: {}. Message to short", conversationId);

        return false;
    }
}
