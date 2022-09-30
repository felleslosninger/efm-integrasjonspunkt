package no.difi.meldingsutveksling.dpi.json;

import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.MessageStatus;

import java.util.function.BiFunction;

public interface MessageStatusDecorator extends BiFunction<Conversation, MessageStatus, MessageStatus> {
}
