package no.difi.meldingsutveksling.dph.client.internal;

import org.springframework.core.io.Resource;

public record WrappedPackage(String forretningsmelding, Resource encryptedAsic) {

    public WrappedPackage(String forretningsmelding) {
        this(forretningsmelding, null);
    }
}
