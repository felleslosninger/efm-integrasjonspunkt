package no.difi.meldingsutveksling.nextmove.nhn;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("Reciever")
public record Reciever(String herid1,String herid2,String patientFnr) implements CommunicationParty {}
