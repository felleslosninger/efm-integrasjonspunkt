package no.difi.meldingsutveksling.nextmove.nhn;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;


public record DPHMessageOut (

    @JsonProperty("messageId")
    String messageId,
    @JsonProperty("conversationId")
        String conversationId,
    @JsonProperty("sender")
     Sender  sender,
    @JsonProperty("reciever")
     Reciever  reciever,
    @JsonProperty("fagmelding")
     String fagmelding
){}



