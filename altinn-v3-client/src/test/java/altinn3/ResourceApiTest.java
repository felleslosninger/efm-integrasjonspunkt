package altinn3;

import com.nimbusds.jose.JOSEException;
import org.apache.http.HttpEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ResourceApiTest {

    @Test
    public void uploadAccessPolicy() throws IOException, InterruptedException, JOSEException {
        String accessToken = ApiUtils.retrieveAccessToken("altinn:resourceregistry/resource.write");
        System.out.println(accessToken);

        String xacml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<xacml:Policy PolicyId=\"urn:altinn:policyid:1\" Version=\"1.0\" RuleCombiningAlgId=\"urn:oasis:names:tc:xacml:3.0:rule-combining-algorithm:deny-overrides\" xmlns:xacml=\"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17\">\n" +
                "  <xacml:Target />\n" +
                "  <xacml:Rule RuleId=\"urn:altinn:resource:eformidling-meldingsteneste-test:ruleid:2\" Effect=\"Permit\">\n" +
                "    <xacml:Description>Post/arkiv og privatperson skal ha tilgang til å lese og abonnere på meldinger</xacml:Description>\n" +
                "    <xacml:Target>\n" +
                "      <xacml:AnyOf>\n" +
                "        <xacml:AllOf>\n" +
                "          <xacml:Match MatchId=\"urn:oasis:names:tc:xacml:3.0:function:string-equal-ignore-case\">\n" +
                "            <xacml:AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">a0236</xacml:AttributeValue>\n" +
                "            <xacml:AttributeDesignator AttributeId=\"urn:altinn:rolecode\" Category=\"urn:oasis:names:tc:xacml:1.0:subject-category:access-subject\" DataType=\"http://www.w3.org/2001/XMLSchema#string\" MustBePresent=\"false\" />\n" +
                "          </xacml:Match>\n" +
                "        </xacml:AllOf>\n" +
                "        <xacml:AllOf>\n" +
                "          <xacml:Match MatchId=\"urn:oasis:names:tc:xacml:3.0:function:string-equal-ignore-case\">\n" +
                "            <xacml:AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">priv</xacml:AttributeValue>\n" +
                "            <xacml:AttributeDesignator AttributeId=\"urn:altinn:rolecode\" Category=\"urn:oasis:names:tc:xacml:1.0:subject-category:access-subject\" DataType=\"http://www.w3.org/2001/XMLSchema#string\" MustBePresent=\"false\" />\n" +
                "          </xacml:Match>\n" +
                "        </xacml:AllOf>\n" +
                "      </xacml:AnyOf>\n" +
                "      <xacml:AnyOf>\n" +
                "        <xacml:AllOf>\n" +
                "          <xacml:Match MatchId=\"urn:oasis:names:tc:xacml:1.0:function:string-equal\">\n" +
                "            <xacml:AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">eformidling-meldingsteneste-test</xacml:AttributeValue>\n" +
                "            <xacml:AttributeDesignator AttributeId=\"urn:altinn:resource\" Category=\"urn:oasis:names:tc:xacml:3.0:attribute-category:resource\" DataType=\"http://www.w3.org/2001/XMLSchema#string\" MustBePresent=\"false\" />\n" +
                "          </xacml:Match>\n" +
                "        </xacml:AllOf>\n" +
                "      </xacml:AnyOf>\n" +
                "      <xacml:AnyOf>\n" +
                "        <xacml:AllOf>\n" +
                "          <xacml:Match MatchId=\"urn:oasis:names:tc:xacml:3.0:function:string-equal-ignore-case\">\n" +
                "            <xacml:AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">read</xacml:AttributeValue>\n" +
                "            <xacml:AttributeDesignator AttributeId=\"urn:oasis:names:tc:xacml:1.0:action:action-id\" Category=\"urn:oasis:names:tc:xacml:3.0:attribute-category:action\" DataType=\"http://www.w3.org/2001/XMLSchema#string\" MustBePresent=\"false\" />\n" +
                "          </xacml:Match>\n" +
                "        </xacml:AllOf>\n" +
                "        <xacml:AllOf>\n" +
                "          <xacml:Match MatchId=\"urn:oasis:names:tc:xacml:3.0:function:string-equal-ignore-case\">\n" +
                "            <xacml:AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">subscribe</xacml:AttributeValue>\n" +
                "            <xacml:AttributeDesignator AttributeId=\"urn:oasis:names:tc:xacml:1.0:action:action-id\" Category=\"urn:oasis:names:tc:xacml:3.0:attribute-category:action\" DataType=\"http://www.w3.org/2001/XMLSchema#string\" MustBePresent=\"false\" />\n" +
                "          </xacml:Match>\n" +
                "        </xacml:AllOf>\n" +
                "      </xacml:AnyOf>\n" +
                "    </xacml:Target>\n" +
                "  </xacml:Rule>\n" +
                "  <xacml:Rule RuleId=\"urn:altinn:resource:eformidling-meldingsteneste-test:ruleid:3\" Effect=\"Permit\">\n" +
                "    <xacml:Description>Digdir skal ha tilgang til å sende meldingar</xacml:Description>\n" +
                "    <xacml:Target>\n" +
                "<!-- Må ha urn:altinn:resource = eformidling-meldingsteneste-test og urn:altinn:org = digdir -->\n" +
                "      <xacml:AnyOf>\n" +
                "        <xacml:AllOf>\n" +
                "          <xacml:Match MatchId=\"urn:oasis:names:tc:xacml:1.0:function:string-equal\">\n" +
                "            <xacml:AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">eformidling-meldingsteneste-test</xacml:AttributeValue>\n" +
                "            <xacml:AttributeDesignator AttributeId=\"urn:altinn:resource\" Category=\"urn:oasis:names:tc:xacml:3.0:attribute-category:resource\" DataType=\"http://www.w3.org/2001/XMLSchema#string\" MustBePresent=\"false\" />\n" +
                "          </xacml:Match>\n" +
                "        </xacml:AllOf>\n" +
                "      </xacml:AnyOf>\n" +
                "<!-- Må ha urn:oasis:names:tc:xacml:1.0:action:action-id = read eller (!?) urn:oasis:names:tc:xacml:1.0:action:action-id = subscribe eller (!?) urn:oasis:names:tc:xacml:1.0:action:action-id = write -->\n" +
                "      <xacml:AnyOf>\n" +
                "        <xacml:AllOf>\n" +
                "          <xacml:Match MatchId=\"urn:oasis:names:tc:xacml:3.0:function:string-equal-ignore-case\">\n" +
                "            <xacml:AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">read</xacml:AttributeValue>\n" +
                "            <xacml:AttributeDesignator AttributeId=\"urn:oasis:names:tc:xacml:1.0:action:action-id\" Category=\"urn:oasis:names:tc:xacml:3.0:attribute-category:action\" DataType=\"http://www.w3.org/2001/XMLSchema#string\" MustBePresent=\"false\" />\n" +
                "          </xacml:Match>\n" +
                "        </xacml:AllOf>\n" +
                "        <xacml:AllOf>\n" +
                "          <xacml:Match MatchId=\"urn:oasis:names:tc:xacml:3.0:function:string-equal-ignore-case\">\n" +
                "            <xacml:AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">subscribe</xacml:AttributeValue>\n" +
                "            <xacml:AttributeDesignator AttributeId=\"urn:oasis:names:tc:xacml:1.0:action:action-id\" Category=\"urn:oasis:names:tc:xacml:3.0:attribute-category:action\" DataType=\"http://www.w3.org/2001/XMLSchema#string\" MustBePresent=\"false\" />\n" +
                "          </xacml:Match>\n" +
                "        </xacml:AllOf>\n" +
                "        <xacml:AllOf>\n" +
                "          <xacml:Match MatchId=\"urn:oasis:names:tc:xacml:3.0:function:string-equal-ignore-case\">\n" +
                "            <xacml:AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">write</xacml:AttributeValue>\n" +
                "            <xacml:AttributeDesignator AttributeId=\"urn:oasis:names:tc:xacml:1.0:action:action-id\" Category=\"urn:oasis:names:tc:xacml:3.0:attribute-category:action\" DataType=\"http://www.w3.org/2001/XMLSchema#string\" MustBePresent=\"false\" />\n" +
                "          </xacml:Match>\n" +
                "        </xacml:AllOf>\n" +
                "      </xacml:AnyOf>\n" +
                "      <xacml:AnyOf>\n" +
                "        <xacml:AllOf>\n" +
                "          <xacml:Match MatchId=\"urn:oasis:names:tc:xacml:1.0:function:string-equal\">\n" +
                "            <xacml:AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">digdir</xacml:AttributeValue>\n" + // NB: dersom test=false ved veksling av token
                //"            <xacml:AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">ttd</xacml:AttributeValue>\n" + // NB: dersom test=true ved veksling av token
                "            <xacml:AttributeDesignator AttributeId=\"urn:altinn:org\" Category=\"urn:oasis:names:tc:xacml:1.0:subject-category:access-subject\" DataType=\"http://www.w3.org/2001/XMLSchema#string\" MustBePresent=\"false\" />\n" +
                "          </xacml:Match>\n" +
                "        </xacml:AllOf>\n" +
                "      </xacml:AnyOf>\n" +
                "    </xacml:Target>\n" +
                "  </xacml:Rule>\n" +
                "  <xacml:ObligationExpressions>\n" +
                "    <xacml:ObligationExpression ObligationId=\"urn:altinn:obligation:authenticationLevel1\" FulfillOn=\"Permit\">\n" +
                "      <xacml:AttributeAssignmentExpression AttributeId=\"urn:altinn:obligation1-assignment1\" Category=\"urn:altinn:minimum-authenticationlevel\">\n" +
                "        <xacml:AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#integer\">3</xacml:AttributeValue>\n" +
                "      </xacml:AttributeAssignmentExpression>\n" +
                "    </xacml:ObligationExpression>\n" +
                "    <xacml:ObligationExpression ObligationId=\"urn:altinn:obligation:authenticationLevel2\" FulfillOn=\"Permit\">\n" +
                "      <xacml:AttributeAssignmentExpression AttributeId=\"urn:altinn:obligation2-assignment2\" Category=\"urn:altinn:minimum-authenticationlevel-org\">\n" +
                "        <xacml:AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#integer\">3</xacml:AttributeValue>\n" +
                "      </xacml:AttributeAssignmentExpression>\n" +
                "    </xacml:ObligationExpression>\n" +
                "  </xacml:ObligationExpressions>\n" +
                "</xacml:Policy>";

        String multipartBoundary = "asdf";
        HttpEntity xacmlHttpEntity = MultipartEntityBuilder.create()
                .addPart("policyFile", new ByteArrayBody(xacml.getBytes(StandardCharsets.UTF_8), "application/xml", "xacml.xml"))
                .setBoundary(multipartBoundary)
                .build();

        InputStream xacmlInputStream = xacmlHttpEntity.getContent();
        byte[] xacmlBinary = new byte[xacmlInputStream.available()];
        xacmlInputStream.read(xacmlBinary);

        HttpRequest httpRequest3 = HttpRequest.newBuilder()
                .uri(URI.create("https://platform.tt02.altinn.no/resourceregistry/api/v1/resource/eformidling-meldingsteneste-test/policy"))
                .header("Authorization", "Bearer " + accessToken)
                //.header("Ocp-Apim-Subscription-Key", "5fb085029c294420ab5c0d1e5a4135e8") // Treng per 13. jan 2025 ikkje i tt02
                .header("Content-Type", "multipart/form-data; boundary=" + multipartBoundary)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(xacmlBinary))
                .build();

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse httpResponse3 = httpClient.send(httpRequest3, HttpResponse.BodyHandlers.ofString());
        System.out.println(httpResponse3.statusCode());
        System.out.println(httpResponse3.body());
        assertEquals(201, httpResponse3.statusCode());
    }

}
