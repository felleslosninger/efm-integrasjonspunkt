package no.difi.messagehandler;

import no.difi.meldingsutveksling.domain.BestEduMessage;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.messagehandler.peppolmessageutils.PeppolMessageMetadata;

import java.util.zip.ZipFile;

public abstract class MessageReceieverTemplate {

    private EventLog eventLog = EventLog.create();

    abstract void sendLeveringskvittering();

    abstract void sendApningskvittering();
    //TODO: Sjekk ut  receive skal ta SBD,her skal ikke være metadata kansje?
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
    //TODO: finn ut om kan det være noe annet enn sbd her,
    // kvittering og melding er ikke begge SBD? om det tales om Ehf eller
    // sbd så trenger jeg ppmmmd?

    private boolean isSBD(PeppolMessageMetadata metaData) {
        return false;
    }

}
