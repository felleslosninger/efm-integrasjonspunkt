package no.difi.meldingsutveksling.dpi.json;

import no.difi.meldingsutveksling.status.MessageStatus;

import java.util.function.Predicate;

public interface DpiMessageStatusFilter extends Predicate<MessageStatus> {
}
