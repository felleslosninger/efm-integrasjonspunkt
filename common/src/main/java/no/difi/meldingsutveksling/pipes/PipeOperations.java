package no.difi.meldingsutveksling.pipes;

import lombok.experimental.UtilityClass;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedOutputStream;
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
}
