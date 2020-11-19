package no.difi.meldingsutveksling.api

interface MessagePolling {
    fun poll()
}

interface DpoPolling: MessagePolling

interface DpePolling: MessagePolling

interface DpfPolling: MessagePolling

interface DpfioPolling: MessagePolling