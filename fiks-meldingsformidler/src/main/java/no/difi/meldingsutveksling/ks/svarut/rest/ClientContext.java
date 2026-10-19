package no.difi.meldingsutveksling.ks.svarut.rest;

import java.util.function.Supplier;

class ClientContext {

    private final ThreadLocal<String> senderOrgs = ThreadLocal.withInitial(() -> null);

    String getSenderOrg() {
        return senderOrgs.get();
    }

    <T> T withSenderOrg(String senderOrg, Supplier<T> supplier) {
        String oldSenderOrg = senderOrgs.get();

        try {
            senderOrgs.set(senderOrg);
            return supplier.get();
        } finally {
            senderOrgs.set(oldSenderOrg);
        }
    }
}
