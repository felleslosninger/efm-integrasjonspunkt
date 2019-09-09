package no.difi.meldingsutveksling.nextmove.message;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

public class AutoClosingInputStream extends PushbackInputStream {

    private final Callback callback;
    private boolean closed = false;

    AutoClosingInputStream(InputStream in, Callback callback) {
        super(in);
        this.callback = callback;
    }

    @Override
    public int available() throws IOException {
        return closed ? 0 : super.available();
    }

    @Override
    public int read() throws IOException {
        return closed ? -1 : closeIfEndOfStream(super.read());
    }

    @Override
    public int read(byte[] b) throws IOException {
        return closed ? -1 : closeIfEndOfStream(super.read(b));
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return closed ? -1 : closeIfEndOfStream(super.read(b, off, len));
    }

    private int closeIfEndOfStream(int read) throws IOException {
        if (closed) {
            return read;
        }

        if (read == -1) {
            close();
            return read;
        }

        int next = super.read();

        if (next == -1) {
            close();
        } else {
            super.unread(next);
        }

        return read;
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            super.close();
            closed = true;
            callback.onClose();
        }
    }

    public boolean isClosed() {
        return closed;
    }

    interface Callback {
        void onClose();
    }
}
