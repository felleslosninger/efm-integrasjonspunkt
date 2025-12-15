package no.difi.meldingsutveksling.nextmove.nhn;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("Receiver")
public record Receiver(String herid1, String herid2, String patientFnr) implements CommunicationParty {}
