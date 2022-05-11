package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class MessageChannelInvalidException extends HttpStatusCodeException {

    public MessageChannelInvalidException(String propsChannel, String sbdChannel) {
        super(HttpStatus.BAD_REQUEST, MessageChannelInvalidException.class.getName(), propsChannel, sbdChannel);
    }
}
