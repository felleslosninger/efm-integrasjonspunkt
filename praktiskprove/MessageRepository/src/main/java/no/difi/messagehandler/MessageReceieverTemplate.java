package no.difi.messagehandler;

import no.difi.meldingsutveksling.domain.BestEduMessage;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;

import java.util.zip.ZipFile;

public abstract class MessageReceieverTemplate {

    private EventLog eventLog = EventLog.create();

    abstract void sendLeveringskvittering();

    abstract void sendApningskvittering();

    void receive(PeppolMessageMetadata metaData, Document document) {

        eventLog.log(new Event());

        if (isSBD(metaData)) {
            sendLeveringskvittering();
            eventLog.log(new Event());

            // depkryptert payload (AES)
            ZipFile asicFile = getZipFileFromDocument(document);
            eventLog.log(new Event());

            // Signaturvalidering
            verifySignature(asicFile);
            eventLog.log(new Event());

            BestEduMessage bestEduMessage = getBestEduFromAsic(asicFile);
            senToNoark(bestEduMessage);
            eventLog.log(new Event());

            sendApningskvittering();
            eventLog.log(new Event());

        } else {
            // Dette er en kvittering
        }
    }

    protected void senToNoark(BestEduMessage bestEduMessage) {
    }

    private BestEduMessage getBestEduFromAsic(ZipFile asicFile) {
        return null;
    }

    private void verifySignature(ZipFile asicFile) {
    }

    private ZipFile getZipFileFromDocument(Document document) {
        return null;
    }

    private boolean isSBD(PeppolMessageMetadata metaData) {
        return false;
    }

}
