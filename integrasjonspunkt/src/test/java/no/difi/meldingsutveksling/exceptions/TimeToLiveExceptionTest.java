package no.difi.meldingsutveksling.exceptions;

import org.junit.Test;

import java.time.ZonedDateTime;

public class TimeToLiveExceptionTest {

    @Test(expected = TimeToLiveException.class)
    public void testingTimeToLiveException(){
        ZonedDateTime time = ZonedDateTime.now();
        throw new TimeToLiveException(time);
    }
}
