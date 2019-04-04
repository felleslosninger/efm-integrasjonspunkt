package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;

/**
 * This is an exception that is used in the message validation to check if ExpectedResponseDateTime in the Standard Business Document Header has Expired.
 * This exception can be thrown when then the message first arrives.
 * Exception: Throws a bad requests and prints the following error message:
 * "ExpectedResponseDateTime (%s) is after current time. Message will not be handled further. Please resend..."
 *
 * @Author Jaflaten
 */
public class TimeToLiveException extends HttpStatusCodeException {
    public  TimeToLiveException(ZonedDateTime time) {
        super(HttpStatus.BAD_REQUEST,
                TimeToLiveException.class.getName(), time);
    }
}