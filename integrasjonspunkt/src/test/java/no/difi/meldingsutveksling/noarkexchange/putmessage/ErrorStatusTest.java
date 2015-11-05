package no.difi.meldingsutveksling.noarkexchange.putmessage;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ErrorStatusTest {

    @Test
    public void organizationCannotRecieveErrorMessage() throws Exception {
        ErrorStatus errorStatus = ErrorStatus.CANNOT_RECIEVE;

        String errorMessage = errorStatus.enduserErrorMessage();

        assertEquals(ErrorStatus.MOTTAKENDE_ORGANISASJON_KAN_IKKE_MOTTA_MELDINGER, errorMessage);
    }

    @Test
    public void missingRecipientLeadsToTekniskFeil() {
        ErrorStatus errorStatus = ErrorStatus.MISSING_RECIPIENT;

        String errorMessage = errorStatus.enduserErrorMessage();

        assertEquals(ErrorStatus.TEKNISK_FEIL, errorMessage);
    }

    @Test
    public void shouldGetTekniskFeilWhenMissingSender() {
        ErrorStatus errorStatus = ErrorStatus.MISSING_SENDER;

        String errorMessage = errorStatus.enduserErrorMessage();

        assertEquals(ErrorStatus.TEKNISK_FEIL, errorMessage);
    }
}