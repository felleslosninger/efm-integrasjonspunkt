package no.difi.meldingsutveksling.transport;

import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import org.springframework.core.env.Environment;

/**
 * Defines a transport. The responsibility of a transport is to receive an SBD ducument and transfer it over some
 * transportation mechanism; oxalis, Altinn, Dropbox or whatever.
 * <p/>
 * See individual modules for implementations of transports.
 *
 * @author Glenn bech
 * @see TransportFactory
 */
public interface Transport {

    /**
     * @param eduDocument An eduDocument with a payload consisting of an CMS encrypted ASIC package
     */
    void send(Environment environment, EduDocument eduDocument);

}
