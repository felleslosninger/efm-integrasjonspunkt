package no.difi.meldingsutveksling.pipes;

import lombok.experimental.UtilityClass;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.function.Consumer;

@UtilityClass
public class PipeOperations {

    public static Consumer<PipedOutputStream> copy(InputStream inputStream) {
        return pos -> {
            try {
                IOUtils.copy(inputStream, pos);
                pos.flush();
            } catch (IOException e) {
                throw new PipeRuntimeException("Copy failed!", e);
            }
        };
    }

    public static Consumer<PipedOutputStream> close(InputStream inputStream) {
        return pos -> {
            try {
                inputStream.close();
            } catch (IOException e) {
                throw new PipeRuntimeException("Closing input stream failed!", e);
            }
        };
    }

    public static Consumer<PipedInputStream> copyTo(OutputStream outputStream) {
        return in -> {
            try {
                IOUtils.copy(in, outputStream);
                outputStream.flush();
            } catch (IOException e) {
                throw new PipeRuntimeException("Copy failed!", e);
            }
        };
    }
}
