package no.difi.meldingsutveksling.fiks.svarinn

import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument
import org.springframework.core.io.Resource

data class SvarInnPackage(val sbd: StandardBusinessDocument, val asic: Resource)