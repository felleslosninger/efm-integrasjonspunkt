package no.difi.meldingsutveksling.queue.service;

import no.difi.meldingsutveksling.queue.rule.Rule;
import no.difi.meldingsutveksling.queue.rule.RuleDefault;
import org.springframework.stereotype.Service;

@Service
public class IntegrasjonspunktQueue {
    private final Rule rule;

    public IntegrasjonspunktQueue() {
        this.rule = new RuleDefault();
    }

    public IntegrasjonspunktQueue(Rule rule) {
        this.rule = rule;
    }

    public void put(String key, String message) {
        //Krypter
        //Valider om ny melding
        //Lagre melding
        //Oppdater status
    }

    public void get() {
        //Oppdater status (aktiv melding)
        //Dekrypter
        //returner melding
    }

    public void put(Object req) {

    }
}
