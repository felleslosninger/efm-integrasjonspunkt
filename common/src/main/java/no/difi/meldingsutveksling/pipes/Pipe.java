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

    private Pipe() {
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

    private void handleCompleteAsync(Void dummy, Throwable t) {
        close();
        if (t != null) {
            log.error("Exception in pipe", t);
            throw new PipeRuntimeException("Exception was thrown in pipe", t);
        }
    }

    public static Pipe of(String description, Consumer<PipedOutputStream> consumer) {
        Pipe pipe = new Pipe();
        CompletableFuture.runAsync(() -> {
            logStart(description);
            consumer.accept(pipe.inlet);
            logFinish(description);
        }).whenCompleteAsync(pipe::handleCompleteAsync);
        return pipe;
    }

    public Pipe andThen(String description, BiConsumer<PipedInputStream, PipedOutputStream> consumer) {
        Pipe newPipe = new Pipe();
        CompletableFuture.runAsync(() -> {
            logStart(description);
            consumer.accept(outlet, newPipe.inlet);
            logFinish(description);
        }).whenCompleteAsync(newPipe::handleCompleteAsync);
        return newPipe;
    }

    public void andFinally(Consumer<PipedInputStream> consumer) {
        consumer.accept(outlet);
        try {
            outlet.close();
        } catch (IOException e) {
            throw new PipeRuntimeException("Could not close outlet", e);
        }
    }

    private static void logStart(String description) {
        log.trace("Starting thread: {}", description);
    }

    private static void logFinish(String description) {
        log.trace("Finished thread: {}", description);
    }
}
