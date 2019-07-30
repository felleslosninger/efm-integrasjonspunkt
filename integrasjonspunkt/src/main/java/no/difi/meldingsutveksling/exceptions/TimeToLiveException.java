package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class TimeToLiveException extends HttpStatusCodeException {
    public TimeToLiveException(OffsetDateTime time) {
        super(HttpStatus.BAD_REQUEST,
                TimeToLiveException.class.getName(),
                DateTimeFormatter.ISO_DATE_TIME.format(time));
    }
}

