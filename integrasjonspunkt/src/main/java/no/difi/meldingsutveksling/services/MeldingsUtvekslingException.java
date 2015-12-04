package no.difi.meldingsutveksling.services;

import no.difi.virksert.client.VirksertClientException;

public class MeldingsUtvekslingException extends Exception {

    public MeldingsUtvekslingException(String message, VirksertClientException e) {

    }
}
