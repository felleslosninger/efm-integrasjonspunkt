package no.difi.meldingsutveksling.ks.mapping;

import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.ks.receipt.DpfReceiptStatus;
import no.difi.meldingsutveksling.ks.svarut.Forsendelse;
import no.difi.meldingsutveksling.ks.svarut.ForsendelseStatus;

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

    public DpfReceiptStatus mapFrom(ForsendelseStatus forsendelseStatus) {
        return forsendelseStatusMapper.mapFrom(forsendelseStatus);
    }
}
