package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.domain.sbdh.EduDocument;

public interface IncommingQueue {
    void enqueueNoark(EduDocument eduDocument);
}
