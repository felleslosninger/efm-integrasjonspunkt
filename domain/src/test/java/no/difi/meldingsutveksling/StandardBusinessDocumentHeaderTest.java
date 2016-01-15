package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.Partner;
import no.difi.meldingsutveksling.domain.sbdh.PartnerIdentification;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kons-gbe on 27.11.2015.
 */

public class StandardBusinessDocumentHeaderTest {

    @Test(expected = MeldingsUtvekslingRuntimeException.class)
    public void testShouldFailOnWrongReceiverListsizeZero() {
        StandardBusinessDocumentHeader header = new StandardBusinessDocumentHeader();
        header.setReceiver(new ArrayList<Partner>());
        header.getReceiverOrganisationNumber();
    }

    @Test(expected = MeldingsUtvekslingRuntimeException.class)
    public void testShouldFailOnWrongReceiverListsizeOneOrMore() {
        StandardBusinessDocumentHeader header = new StandardBusinessDocumentHeader();
        Partner p = new Partner();
        List<Partner> list = new ArrayList<Partner>();
        list.add(p);
        list.add(p);
        header.setReceiver(list);
        header.getReceiverOrganisationNumber();
    }

    @Test(expected = MeldingsUtvekslingRuntimeException.class)
    public void testMissingIdentifierOnPartner() {
        StandardBusinessDocumentHeader header = new StandardBusinessDocumentHeader();
        Partner p = new Partner();
        List<Partner> list = new ArrayList<Partner>();
        list.add(p);
        header.setReceiver(list);
        header.getReceiverOrganisationNumber();
    }

    @Test
    public void positiveTest() {
        StandardBusinessDocumentHeader header = new StandardBusinessDocumentHeader();
        Partner p = new Partner();
        final PartnerIdentification value = new PartnerIdentification();
        value.setAuthority("authorotai");
        value.setValue("011076111111");
        p.setIdentifier(value);
        List<Partner> list = new ArrayList<Partner>();
        list.add(p);
        header.setReceiver(list);
        header.getReceiverOrganisationNumber();
    }

}