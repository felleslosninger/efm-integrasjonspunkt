package no.difi.meldingsutveksling.ks.svarinn;

import java.io.FilterInputStream;
import java.io.InputStream;

public class NonClosableInputStream extends FilterInputStream {

    NonClosableInputStream(InputStream is) {
        super(is);
    }

    /**
     * The whole point of this input stream is to ignore invocations to close()
     */
    @Override
    public void close() {

    }
}