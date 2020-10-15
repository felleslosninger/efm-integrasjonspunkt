package no.difi.meldingsutveksling.fiks.svarinn

import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument
import java.io.InputStream

data class SvarInnPackage(val sbd: StandardBusinessDocument, val asicStream: InputStream)