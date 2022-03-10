package no.difi.meldingsutveksling.pipes;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
public class Pipe {

    private final Executor executor;
    private final Reject reject;
    private final PipedOutputStream inlet;
    private final PipedInputStream outlet;

    private Pipe(Executor executor, Reject reject) {
        this.executor = executor;
        this.reject = reject;
        this.inlet = new PipedOutputStream();
        this.outlet = new PipedInputStream(32768);
        connectInletAndOutlet();
    }

    private void connectInletAndOutlet() {
        try {
            inlet.connect(outlet);
        } catch (IOException e) {
            reject.reject(e);
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
            reject.reject(e);
        }
    }

    @SuppressWarnings("squir:S1172")
    private void handleComplete(Void v, Throwable t) {
        close();
        if (t != null) {
            if (t instanceof CompletionException) {
                CompletionException ce = (CompletionException) t;
                reject.reject(ce.getCause());
            } else {
                reject.reject(t);
            }
        }
    }

    public static Pipe of(Executor executor, String description, Consumer<PipedOutputStream> consumer, Reject reject) {
        Pipe pipe = new Pipe(executor, reject);
        logBeforeThread(description);
        CompletableFuture.runAsync(() -> {
            logStart(description);
            consumer.accept(pipe.inlet);
            logFinish(description);
        }, executor).whenComplete(pipe::handleComplete);
        return pipe;
    }

    public Pipe andThen(String description, BiConsumer<PipedInputStream, PipedOutputStream> consumer) {
        Pipe newPipe = new Pipe(executor, reject);
        logBeforeThread(description);
        CompletableFuture.runAsync(() -> {
            logStart(description);
            consumer.accept(outlet, newPipe.inlet);
            logFinish(description);
        }, executor).whenComplete(newPipe::handleComplete);
        return newPipe;
    }

    public void andFinally(Consumer<PipedInputStream> consumer) {
        consumer.accept(outlet);
        try {
            outlet.close();
        } catch (IOException e) {
            reject.reject(e);
        }
    }

    private static void logBeforeThread(String description) {
        log.trace("Before thread: {}", description);
    }

    private static void logStart(String description) {
        log.trace("Starting thread: {}", description);
    }

    private static void logFinish(String description) {
        log.trace("Finished thread: {}", description);
    }
}
