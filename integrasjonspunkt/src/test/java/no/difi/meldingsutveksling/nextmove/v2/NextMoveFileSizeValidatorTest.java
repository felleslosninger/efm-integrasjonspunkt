package no.difi.meldingsutveksling.nextmove.v2;

import jakarta.servlet.http.HttpServletRequest;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.AltinnFormidlingsTjenestenConfig;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.exceptions.MaxFileSizeExceededException;
import no.difi.meldingsutveksling.nextmove.BusinessMessageFile;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.unit.DataSize;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NextMoveFileSizeValidatorTest {

    @Mock
    private IntegrasjonspunktProperties props;

    @Mock
    private NextMoveOutMessage msg;

    @Mock
    private HttpServletRequest req;

    private NextMoveFileSizeValidator validator;

    private NextMoveUploadedFile file;

    @BeforeEach
    void before() {
        AltinnFormidlingsTjenestenConfig dpo = mock(AltinnFormidlingsTjenestenConfig.class);
        when(props.getDpo()).thenReturn(dpo);
        when(dpo.getUploadSizeLimit()).thenReturn(DataSize.parse("10MB"));

        validator = new NextMoveFileSizeValidator(props);

        when(msg.getServiceIdentifier()).thenReturn(ServiceIdentifier.DPO);
        when(msg.getFiles()).thenReturn(java.util.Collections.emptySet());

        file = new NextMoveUploadedFile("text/html", "attachment; filename=\"test.txt\"", "title", req);
    }

    @Test
    void test_upload_is_within_limit() {
        when(req.getContentLengthLong()).thenReturn(DataSize.parse("5MB").toBytes());
        validator.validate(msg, file);
    }

    @Test
    void test_multiple_uploads_is_within_limit_size() {
        BusinessMessageFile existingFile = mock(BusinessMessageFile.class);
        when(existingFile.getSize()).thenReturn(DataSize.parse("4MB").toBytes());
        when(msg.getFiles()).thenReturn(java.util.Set.of(existingFile));
        when(req.getContentLengthLong()).thenReturn(DataSize.parse("5MB").toBytes());
        validator.validate(msg, file);
    }

    @Test
    void test_upload_exceeds_limit_size() {
        when(req.getContentLengthLong()).thenReturn(DataSize.parse("100MB").toBytes());
        assertThrows(MaxFileSizeExceededException.class, () -> validator.validate(msg, file));
    }

    @Test
    void test_multiple_uploads_exceed_limit_size() {
        BusinessMessageFile existingFile = mock(BusinessMessageFile.class);
        when(existingFile.getSize()).thenReturn(DataSize.parse("6MB").toBytes());
        when(msg.getFiles()).thenReturn(java.util.Set.of(existingFile));
        when(req.getContentLengthLong()).thenReturn(DataSize.parse("5MB").toBytes());
        assertThrows(MaxFileSizeExceededException.class, () -> validator.validate(msg, file));
    }
}
