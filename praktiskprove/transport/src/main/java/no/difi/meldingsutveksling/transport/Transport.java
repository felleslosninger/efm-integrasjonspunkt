package no.difi.meldingsutveksling.transport;

import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

/**
 * Defines a transport. The responsibility of a transport is to receive an SBD ducument and transfer it over some
 * transportation mechanism; oxalis, Altinn, Dropbox or whatever.
 * <p/>
 * New maven modules are created for individual transports.
 *
 * @author Glenn bech
 * @see TransportFactory
 */
public interface Transport {

    /**
     * @param document An SBD document with a payload consisting of an CMS encrypted ASIC package
     */
    public void send(StandardBusinessDocument document);

}
