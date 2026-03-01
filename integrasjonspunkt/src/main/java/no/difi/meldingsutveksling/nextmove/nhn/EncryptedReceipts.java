package no.difi.meldingsutveksling.nextmove.nhn;

import no.difi.meldingsutveksling.domain.EncryptedBusinessMessage;

import java.util.HashMap;

public record EncryptedReceipts( EncryptedBusinessMessage receipts,HashMap<String, String> signature) {
}
