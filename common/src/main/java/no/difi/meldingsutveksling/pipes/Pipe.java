package no.difi.meldingsutveksling.pipes;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
public class Pipe {

    private final PipedOutputStream inlet;
    private final PipedInputStream outlet;

    public Pipe() {
        this.inlet = new PipedOutputStream();
        this.outlet = new PipedInputStream();
        connectInletAndOutlet();
    }

    private void connectInletAndOutlet() {
        try {
            inlet.connect(outlet);
        } catch (IOException e) {
            throw new PipeRuntimeException("Connect failed!", e);
        }
    }

    public PipedInputStream outlet() {
        return outlet;
    }

    private void close() {
        try {
            inlet.flush();
            inlet.close();
        } catch (IOException e) {
            throw new PipeRuntimeException("Could not close of", e);
        }
    }

    public Pipe consume(String description, Consumer<PipedOutputStream> consumer) {
        CompletableFuture.runAsync(() -> {
            log.trace("Starting thread: {}", description);
            consumer.accept(inlet);
            log.trace("Thread finished: {}", description);
        }).whenCompleteAsync((dummy, ex) -> close());
        return this;
    }

    public static Pipe of(String description, Consumer<PipedOutputStream> consumer) {
        Pipe pipe = new Pipe();
        return pipe.consume(description, consumer);
    }

    public Pipe andThen(String description, BiConsumer<PipedInputStream, PipedOutputStream> consumer) {
        Pipe newPipe = new Pipe();
        CompletableFuture.runAsync(() -> {
            log.trace("Starting thread: {}", description);
            consumer.accept(outlet, newPipe.inlet);
            log.trace("Thread finished: {}", description);
        }).whenCompleteAsync((dummy, ex) -> newPipe.close());
        return newPipe;
    }
}
