package no.difi.meldingsutveksling.exceptions;

import no.difi.meldingsutveksling.DocumentType;
import org.springframework.http.HttpStatus;

public class DocumentTypeDoNotFitDocumentStandardException extends HttpStatusCodeException {

    public DocumentTypeDoNotFitDocumentStandardException(DocumentType documentType, String standard) {
        super(HttpStatus.BAD_REQUEST, DocumentTypeDoNotFitDocumentStandardException.class.getName(), documentType.getType(), standard);
    }
}
