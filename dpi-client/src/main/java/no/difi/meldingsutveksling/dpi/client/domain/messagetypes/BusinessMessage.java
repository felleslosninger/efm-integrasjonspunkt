package no.difi.meldingsutveksling.dpi.client.domain.messagetypes;

import no.difi.meldingsutveksling.dpi.client.domain.sbd.Avsender;

public interface BusinessMessage {

    /**
     * Sender. When sending on behalf of an organization, this field will contain the "on behalf of"-organization.
     */
    Avsender getAvsender();
}
