package no.difi.meldingsutveksling.fiks.svarinn

import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument
import no.difi.move.common.io.InMemoryWithTempFileFallbackResource
import org.springframework.core.io.InputStreamResource

data class SvarInnPackage(val sbd: StandardBusinessDocument, val asic: InputStreamResource)