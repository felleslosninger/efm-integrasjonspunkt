package no.difi.meldingsutveksling;

/**
 * Indicates that a task is potentially big and resource demanding and should not be executed in paralell with other
 * big and resource demanding tasks
 */
public interface MessageDownloaderModule {
    void downloadFiles();
}
