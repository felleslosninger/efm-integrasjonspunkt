package no.difi.meldingsutveksling.eventlog;

/**
 * Created by kubkaray on 16.12.2014.
 */
public class CustomRuntimeExceptions extends RuntimeException{

    public CustomRuntimeExceptions(Exception e) {
        super(e);
    }
}
