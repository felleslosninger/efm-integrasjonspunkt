package no.difi.virksert.scope;

import no.difi.virksert.api.Scope;

public abstract class AbstractScope implements Scope {

    @Override
    public String[] getRootCertificateKeys() {
        return new String[] {"buypass-root", "commfides-root"};
    }

    @Override
    public String[] getIntermediateCertificateKeys() {
        return new String[] {"buypass-intermediate", "commfides-intermediate"};
    }
}
