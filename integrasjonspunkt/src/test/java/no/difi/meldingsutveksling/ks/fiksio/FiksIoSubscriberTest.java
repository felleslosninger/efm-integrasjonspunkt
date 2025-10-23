package no.difi.meldingsutveksling.ks.fiksio;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.NextMoveQueue;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.sbd.SBDFactory;
import no.ks.fiks.io.client.FiksIOKlient;
import no.ks.fiks.io.client.SvarSender;
import no.ks.fiks.io.client.model.MeldingId;
import no.ks.fiks.io.client.model.MottattMelding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class FiksIoSubscriberTest {

    @Mock
    private FiksIOKlient fiksIOKlient;
    @Mock
    private NextMoveQueue nextMoveQueue;
    @Mock
    private SBDFactory sbdFactory;
    @Mock
    private IntegrasjonspunktProperties props;

    private FiksIoSubscriber fiksIoSubscriber;

    @BeforeEach
    void before() {
        doNothing().when(fiksIOKlient).newSubscription(any());
        IntegrasjonspunktProperties.Organization org = new IntegrasjonspunktProperties.Organization().setNumber("123123123");
        when(props.getOrg()).thenReturn(org);
        no.difi.meldingsutveksling.config.FiksConfig fiks = mock(no.difi.meldingsutveksling.config.FiksConfig.class);
        no.difi.meldingsutveksling.config.FiksConfig.FiksIO io = new no.difi.meldingsutveksling.config.FiksConfig.FiksIO();
        io.setSenderOrgnr("321321321");
        when(props.getFiks()).thenReturn(fiks);
        when(fiks.getIo()).thenReturn(io);

        when(sbdFactory.createNextMoveSBD(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(new StandardBusinessDocument());
        doNothing().when(nextMoveQueue).enqueueIncomingMessage(any(), any(), any());

        fiksIoSubscriber = new FiksIoSubscriber(fiksIOKlient, sbdFactory, props, nextMoveQueue);
    }

    @Test
    void test_handle_message() throws Exception {
        MottattMelding mottattMelding = mock(MottattMelding.class);
        when(mottattMelding.getMeldingId()).thenReturn(new MeldingId(UUID.fromString("6d16a689-da59-4d22-8e3e-82bcb9169ccb")));
        when(mottattMelding.getMeldingType()).thenReturn("no.digdir.einnsyn.v1");
        when(mottattMelding.getKryptertStream()).thenReturn(mock(InputStream.class));

        SvarSender svarSender = mock(SvarSender.class, RETURNS_DEEP_STUBS);

        Method m = FiksIoSubscriber.class.getDeclaredMethod("handleMessage", MottattMelding.class, SvarSender.class);
        m.setAccessible(true);
        m.invoke(fiksIoSubscriber, mottattMelding, svarSender);

        verify(nextMoveQueue).enqueueIncomingMessage(any(), eq(ServiceIdentifier.DPFIO), any());
        verify(svarSender).ack();
    }

}
