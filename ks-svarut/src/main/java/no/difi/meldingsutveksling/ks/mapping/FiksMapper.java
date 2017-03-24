package no.difi.meldingsutveksling.ks.mapping;

import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.ks.Forsendelse;
import no.difi.meldingsutveksling.ks.ForsendelseStatus;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;

import java.security.cert.X509Certificate;

public class FiksMapper {
    private ForsendelseMapper forsendelseMapper;
    private ForsendelseStatusMapper forsendelseStatusMapper;

    public FiksMapper(ForsendelseMapper forsendelseMapper, ForsendelseStatusMapper forsendelseStatusMapper) {
        this.forsendelseMapper = forsendelseMapper;
        this.forsendelseStatusMapper = forsendelseStatusMapper;
    }

    public Forsendelse mapFrom(EDUCore eduCore, X509Certificate certificate) {
        return forsendelseMapper.mapFrom(eduCore, certificate);
    }

    public ReceiptStatus mapFrom(ForsendelseStatus forsendelseStatus) {
        return forsendelseStatusMapper.mapFrom(forsendelseStatus);
    }
}
