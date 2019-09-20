package no.difi.meldingsutveksling;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.activation.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


@Data
@AllArgsConstructor(staticName = "of")
public class InputStreamDataSource implements DataSource {

    private InputStream is;

    @Override
    public InputStream getInputStream() throws IOException {
        return this.is;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getContentType() {
        return "application/octet-stream";
    }

    @Override
    public String getName() {
        return "InputStreamDataSource";
    }
}
