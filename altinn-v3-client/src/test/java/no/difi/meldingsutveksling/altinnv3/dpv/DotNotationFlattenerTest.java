package no.difi.meldingsutveksling.altinnv3.dpv;

import no.digdir.altinn3.correspondence.model.BaseCorrespondenceExt;
import no.digdir.altinn3.correspondence.model.InitializeCorrespondenceAttachmentExt;
import no.digdir.altinn3.correspondence.model.InitializeCorrespondenceContentExt;
import no.digdir.altinn3.correspondence.model.InitializeCorrespondencesExt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
    DotNotationFlattener.class
})
public class DotNotationFlattenerTest {

    @Autowired
    private DotNotationFlattener dotNotationFlattener;

    @Test
    public void flatten() {
        Map<String, String> expected = Map.of(
            "correspondence.content.messageTitle", "title",
            "correspondence.resourceId", "resourceId",
            "correspondence.isConfidential", "true",
            "correspondence.dueDateTime", "2025-06-01T10:38:23+02:00",
            "correspondence.content.attachments[0].fileName", "attachment1",
            "correspondence.content.attachments[1].fileName", "attachment2"
            );

        InitializeCorrespondencesExt obj = new InitializeCorrespondencesExt();
        BaseCorrespondenceExt baseCorrespondenceExt = new BaseCorrespondenceExt();
        InitializeCorrespondenceContentExt initializeCorrespondenceContentExt = new InitializeCorrespondenceContentExt();
        InitializeCorrespondenceAttachmentExt attachment1 = new InitializeCorrespondenceAttachmentExt();
        InitializeCorrespondenceAttachmentExt attachment2 = new InitializeCorrespondenceAttachmentExt();

        attachment1.setFileName("attachment1");
        attachment2.setFileName("attachment2");

        initializeCorrespondenceContentExt.setMessageTitle("title");
        initializeCorrespondenceContentExt.setAttachments(List.of(attachment1, attachment2));

        baseCorrespondenceExt.setIsConfidential(true);
        baseCorrespondenceExt.setResourceId("resourceId");
        baseCorrespondenceExt.setDueDateTime(OffsetDateTime.parse("2025-06-01T10:38:23+02:00"));
        baseCorrespondenceExt.setContent(initializeCorrespondenceContentExt);

        obj.setCorrespondence(baseCorrespondenceExt);

        Map<String, String> result = dotNotationFlattener.flatten(obj);

        assertThat(result).containsExactlyInAnyOrderEntriesOf(expected);
    }
}
