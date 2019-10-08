package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.noarkexchange.putmessage.ErrorStatus;

import java.util.AbstractCollection;
import java.util.ArrayList;

public class MessageContext {
    private Avsender avsender;
    private Mottaker mottaker;
    private AbstractCollection<ErrorStatus> errors;

    public MessageContext() {
        errors = new ArrayList<>();
    }

    public Avsender getAvsender() {
        return avsender;
    }

    public void setAvsender(Avsender avsender) {
        this.avsender = avsender;
    }

    public Mottaker getMottaker() {
        return mottaker;
    }

    public void setMottaker(Mottaker mottaker) {
        this.mottaker = mottaker;
    }

    /**
     * Appends errorStatus to IntegrasjonspunktContext error statuses
     *
     * @param errorStatus
     */
    public void addError(ErrorStatus errorStatus) {
        this.errors.add(errorStatus);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public AbstractCollection<ErrorStatus> getErrors() {
        return errors;
    }

}
