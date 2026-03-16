package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class MissingJournalpostFieldException extends HttpStatusCodeException {

    public MissingJournalpostFieldException(String field) {
        super(HttpStatus.BAD_REQUEST, MissingJournalpostFieldException.class.getName(), field);
    }
}
