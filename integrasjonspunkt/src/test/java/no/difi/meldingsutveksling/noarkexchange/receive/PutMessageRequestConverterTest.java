package no.difi.meldingsutveksling.noarkexchange.receive;

import no.difi.meldingsutveksling.noarkexchange.PutMessageRequestConverter;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PutMessageRequestConverterTest {

    private final PutMessageRequestType putMessageRequestType = new PutMessageRequestType();

    @BeforeEach
    public void setup() {
        putMessageRequestType.setPayload(new AppReceiptType());
    }

    @Test
    public void testMarshallKvittering() throws Exception {
        PutMessageRequestConverter converter = new PutMessageRequestConverter();
        converter.marshallToBytes(putMessageRequestType);
    }

    @Test
    public void testUnmarshallKvittering() throws Exception {
        PutMessageRequestConverter converter = new PutMessageRequestConverter();
        converter.unmarshallFrom(converter.marshallToBytes(putMessageRequestType));
    }

}