package no.difi.meldingsutveksling.nextmove;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class NextMoveClientInputException extends RuntimeException {
    public NextMoveClientInputException(String s) {
        super(s);
    }
}
