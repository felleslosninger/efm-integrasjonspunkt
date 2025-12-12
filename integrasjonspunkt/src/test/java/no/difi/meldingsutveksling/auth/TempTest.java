package no.difi.meldingsutveksling.auth;

import org.junit.jupiter.api.Test;

public class TempTest {

    @Test
    public void testPLaceholder() {
         String MESSAGE_RECEIPT_PATH =  "/in/%s/receipt";

         System.out.println(MESSAGE_RECEIPT_PATH.formatted("43545354"));

    }
}
