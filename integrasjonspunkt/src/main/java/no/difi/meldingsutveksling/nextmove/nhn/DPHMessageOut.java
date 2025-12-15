package no.difi.meldingsutveksling.nextmove.nhn;


import com.fasterxml.jackson.annotation.JsonProperty;


public record DPHMessageOut (

    @JsonProperty("messageId")
    String messageId,
    @JsonProperty("conversationId")
    String conversationId,
    @JsonProperty("onBehalfOfOrgNum")
    String onBehalfOfOrgNum,
    @JsonProperty("sender")
    Sender  sender,
    @JsonProperty("receiver")
    Receiver receiver,
    @JsonProperty("fagmelding")
    String fagmelding,
    @JsonProperty("vedlegg")
    String vedlegg

){};





