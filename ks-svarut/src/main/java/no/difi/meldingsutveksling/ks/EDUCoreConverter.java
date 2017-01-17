package no.difi.meldingsutveksling.ks;

import no.difi.meldingsutveksling.core.EDUCore;

/**
 * Created by mfhoel on 15.12.2016.
 */
public interface EDUCoreConverter {
    Forsendelse convert(EDUCore domainMessage);
}
