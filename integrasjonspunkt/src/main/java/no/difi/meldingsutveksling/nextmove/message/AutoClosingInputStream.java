package no.difi.meldingsutveksling.nextmove.message;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;

@RequiredArgsConstructor
public class AutoClosingInputStream extends InputStream {

    private final InputStream delegate;
    private final Callback callback;
    private boolean closed = false;

    @Override
    public int read(byte[] b) throws IOException {
        int read = delegate.read(b);
        if (read < b.length) {
            close();
        }
        return read;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = delegate.read(b, off, len);
        if (read < b.length) {
            close();
        }
        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        return delegate.skip(n);
    }

    @Override
    public int available() throws IOException {
        return delegate.available();
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            delegate.close();
            closed = true;
            callback.onClose();
        }
    }

    @Override
    public synchronized void mark(int readlimit) {
        delegate.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        delegate.reset();
    }

    @Override
    public boolean markSupported() {
        return delegate.markSupported();
    }

    @Override
    public int read() throws IOException {
        return delegate.read();
    }

    interface Callback {
        void onClose();
    }
}
