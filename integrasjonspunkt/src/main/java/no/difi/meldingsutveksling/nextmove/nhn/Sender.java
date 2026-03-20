package no.difi.meldingsutveksling.nextmove.nhn;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
interface CommunicationParty{
    String herid1();
    String herid2();
}

@JsonTypeName("Sender")
public record Sender(String herid1,String herid2,String name) implements CommunicationParty{}
