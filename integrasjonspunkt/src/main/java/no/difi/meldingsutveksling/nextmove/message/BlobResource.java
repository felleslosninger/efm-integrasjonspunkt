package no.difi.meldingsutveksling.nextmove.message;

import org.springframework.core.io.AbstractResource;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;

public class BlobResource extends AbstractResource {

    private final Blob blob;
    private final String description;

    public BlobResource(Blob blob, @Nullable String description) {
        this.blob = blob;
        this.description = (description != null ? description : "");
    }

    public String getDescription() {
        return "BLOB resource [" + this.description + "]";
    }

    @Override
    public InputStream getInputStream() throws IOException {
        try {
            return blob.getBinaryStream();
        } catch (SQLException e) {
            throw new IOException("Could not read BLOB!", e);
        }
    }
}
