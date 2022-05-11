package no.difi.meldingsutveksling.dpi.client.domain.messagetypes;

import no.difi.meldingsutveksling.dpi.client.domain.sbd.Dokumentpakkefingeravtrykk;

public interface DokumentpakkefingeravtrykkHolder {

    Dokumentpakkefingeravtrykk getDokumentpakkefingeravtrykk();

    BusinessMessage setDokumentpakkefingeravtrykk(Dokumentpakkefingeravtrykk dokumentpakkefingeravtrykk);
}
