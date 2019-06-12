package no.difi.meldingsutveksling.pipes;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.PipedOutputStream;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class Plumber {

    private final ThreadPoolTaskExecutor taskExecutor;

    public Pipe pipe(String description, Consumer<PipedOutputStream> consumer) {
        return Pipe.of(taskExecutor, description, consumer);
    }
}
