package no.difi.meldingsutveksling.pipes;

import lombok.RequiredArgsConstructor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class PromiseMaker {

    private final TaskExecutor taskExecutor;

    public void awaitVoid(Consumer<Reject> rejectConsumer) {
        new Promise<Void>((resolve, reject) -> {
            try {
                rejectConsumer.accept(reject);
                resolve.resolve(null);
            } catch (Exception e) {
                reject.reject(e);
            }
        }, taskExecutor).await();
    }

    public <T> T await(Function<Reject, T> action) {
        return new Promise<T>((resolve, reject) -> {
            try {
                resolve.resolve(action.apply(reject));
            } catch (Exception e) {
                reject.reject(e);
            }
        }, taskExecutor).await();
    }
}
