package no.difi.meldingsutveksling.noarkexchange.receive;

/**
 * Interface for converting things like message types to Payload object
 * @param <T>
 */
public interface PayloadConverter<T> {
    Object marshallToPayload(T message);
}
