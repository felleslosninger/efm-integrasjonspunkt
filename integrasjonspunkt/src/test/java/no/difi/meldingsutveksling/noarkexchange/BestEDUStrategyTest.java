package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import org.junit.Test;
import org.mockito.Mockito;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test for hte AppReceiptStrategy
 *
 * @author Glenn Bech
 */

public class BestEDUStrategyTest {


    private String ePhorteStyle = "    <soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:typ=\"http://www.arkivverket.no/Noark/Exchange/types\">\n" +
            "        <soapenv:Header/>\n" +
            "        <soapenv:Body>\n" +
            "            <typ:PutMessageRequest>\n" +
            "                <envelope conversationId=\"?\" contentNamespace=\"?\">\n" +
            "                    <sender>\n" +
            "                        <orgnr>974720760</orgnr>\n" +
            "                        <!--Optional:-->\n" +
            "                        <name>?</name>\n" +
            "                        <!--Optional:-->\n" +
            "                        <email>?</email>\n" +
            "                        <!--Optional:-->\n" +
            "                        <ref>?</ref>\n" +
            "                    </sender>\n" +
            "                    <receiver>\n" +
            "                        <orgnr>987464291</orgnr>\n" +
            "                        <!--Optional:-->\n" +
            "                        <name>?</name>\n" +
            "                        <!--Optional:-->\n" +
            "                        <email>?</email>\n" +
            "                        <!--Optional:-->\n" +
            "                        <ref>?</ref>\n" +
            "                    </receiver>\n" +
            "                </envelope>\n" +
            "                <payload class=\"com.sun.org.apache.xerces.internal.dom.ElementNSImpl\" serialization=\"custom\">\n" +
            "                    <com.sun.org.apache.xerces.internal.dom.NodeImpl>\n" +
            "                        <default>\n" +
            "                            <flags>24</flags>\n" +
            "                            <ownerNode class=\"com.sun.org.apache.xerces.internal.dom.DocumentImpl\" serialization=\"custom\">\n" +
            "                                <com.sun.org.apache.xerces.internal.dom.NodeImpl>\n" +
            "                                    <default>\n" +
            "                                        <flags>0</flags>\n" +
            "                                    </default>\n" +
            "                                </com.sun.org.apache.xerces.internal.dom.NodeImpl>\n" +
            "                                <com.sun.org.apache.xerces.internal.dom.ParentNode>best\n" +
            "                                    <default>\n" +
            "                                        <firstChild class=\"com.sun.org.apache.xerces.internal.dom.ElementNSImpl\" reference=\"../../../../../..\"/>\n" +
            "                                        <ownerDocument class=\"com.sun.org.apache.xerces.internal.dom.DocumentImpl\" reference=\"../../..\"/>\n" +
            "                                    </default>\n" +
            "                                </com.sun.org.apache.xerces.internal.dom.ParentNode>\n" +
            "                                <com.sun.org.apache.xerces.internal.dom.CoreDocumentImpl>\n" +
            "                                    <default>\n" +
            "                                        <allowGrammarAccess>false</allowGrammarAccess>\n" +
            "                                        <ancestorChecking>true</ancestorChecking>\n" +
            "                                        <changes>2</changes>\n" +
            "                                        <documentNumber>0</documentNumber>\n" +
            "                                        <errorChecking>true</errorChecking>\n" +
            "                                        <nodeCounter>0</nodeCounter>\n" +
            "                                        <standalone>false</standalone>\n" +
            "                                        <xml11Version>false</xml11Version>\n" +
            "                                        <xmlVersionChanged>false</xmlVersionChanged>\n" +
            "                                        <docElement class=\"com.sun.org.apache.xerces.internal.dom.ElementNSImpl\" reference=\"../../../../../..\"/>\n" +
            "                                    </default>\n" +
            "                                </com.sun.org.apache.xerces.internal.dom.CoreDocumentImpl>\n" +
            "                                <com.sun.org.apache.xerces.internal.dom.DocumentImpl>\n" +
            "                                    <default>\n" +
            "                                        <mutationEvents>false</mutationEvents>\n" +
            "                                    </default>\n" +
            "                                </com.sun.org.apache.xerces.internal.dom.DocumentImpl>\n" +
            "                            </ownerNode>\n" +
            "                        </default>\n" +
            "                    </com.sun.org.apache.xerces.internal.dom.NodeImpl>\n" +
            "                    <com.sun.org.apache.xerces.internal.dom.ChildNode>\n" +
            "                        <default>\n" +
            "                            <previousSibling class=\"com.sun.org.apache.xerces.internal.dom.ElementNSImpl\" reference=\"../../..\"/>\n" +
            "                        </default>\n" +
            "                    </com.sun.org.apache.xerces.internal.dom.ChildNode>\n" +
            "                    <com.sun.org.apache.xerces.internal.dom.ParentNode>\n" +
            "                        <default>\n" +
            "                            <firstChild class=\"com.sun.org.apache.xerces.internal.dom.TextImpl\" serialization=\"custom\">\n" +
            "                                <com.sun.org.apache.xerces.internal.dom.NodeImpl>\n" +
            "                                    <default>\n" +
            "                                        <flags>24</flags>\n" +
            "                                        <ownerNode class=\"com.sun.org.apache.xerces.internal.dom.ElementNSImpl\" reference=\"../../../../../..\"/>\n" +
            "                                    </default>\n" +
            "                                </com.sun.org.apache.xerces.internal.dom.NodeImpl>\n" +
            "                                <com.sun.org.apache.xerces.internal.dom.ChildNode>\n" +
            "                                    <default>\n" +
            "                                        <previousSibling class=\"com.sun.org.apache.xerces.internal.dom.TextImpl\" reference=\"../../..\"/>\n" +
            "                                    </default>\n" +
            "                                </com.sun.org.apache.xerces.internal.dom.ChildNode>\n" +
            "                                <com.sun.org.apache.xerces.internal.dom.CharacterDataImpl>\n" +
            "                                    <default>\n" +
            "                                        <data>&lt;?xml version=&quot;1.0&quot; encoding=&quot;utf-8&quot;?&gt;&lt;Melding xmlns:xsd=&quot;http://www.w3.org/2001/XMLSchema&quot; xmlns:xsi=&quot;http://www.w3.org/2001/XMLSchema-instance&quot; xmlns=&quot;http://www.arkivverket.no/Noark4-1-WS-WD/types&quot;&gt;&lt;journpost xmlns=&quot;&quot;&gt;&lt;jpId&gt;219759&lt;/jpId&gt;&lt;jpJaar&gt;2014&lt;/jpJaar&gt;&lt;jpSeknr&gt;11686&lt;/jpSeknr&gt;&lt;jpJpostnr&gt;1&lt;/jpJpostnr&gt;&lt;jpJdato&gt;2014-11-27&lt;/jpJdato&gt;&lt;jpNdoktype&gt;U&lt;/jpNdoktype&gt;&lt;jpDokdato&gt;2014-11-27&lt;/jpDokdato&gt;&lt;jpStatus&gt;F&lt;/jpStatus&gt;&lt;jpInnhold&gt;Test MSH Difi&lt;/jpInnhold&gt;&lt;jpU1&gt;0&lt;/jpU1&gt;&lt;jpForfdato /&gt;&lt;jpTgkode /&gt;&lt;jpUoff /&gt;&lt;jpAgdato /&gt;&lt;jpAgkode /&gt;&lt;jpSaksdel /&gt;&lt;jpU2&gt;0&lt;/jpU2&gt;&lt;jpArkdel /&gt;&lt;jpTlkode /&gt;&lt;jpAntved&gt;0&lt;/jpAntved&gt;&lt;jpSaar&gt;2014&lt;/jpSaar&gt;&lt;jpSaseknr&gt;2703&lt;/jpSaseknr&gt;&lt;jpOffinnhold&gt;Test MSH Difi&lt;/jpOffinnhold&gt;&lt;jpTggruppnavn /&gt;&lt;avsmot&gt;&lt;amId&gt;501041&lt;/amId&gt;&lt;amOrgnr&gt;974764687&lt;/amOrgnr&gt;&lt;amIhtype&gt;1&lt;/amIhtype&gt;&lt;amKopimot&gt;0&lt;/amKopimot&gt;&lt;amBehansv&gt;0&lt;/amBehansv&gt;&lt;amNavn&gt;Fylkesmannen i Nordland&lt;/amNavn&gt;&lt;amU1&gt;0&lt;/amU1&gt;&lt;amKortnavn&gt;FMNO&lt;/amKortnavn&gt;&lt;amAdresse&gt;Moloveien 10&lt;/amAdresse&gt;&lt;amPostnr&gt;8002&lt;/amPostnr&gt;&lt;amPoststed&gt;BODO&lt;/amPoststed&gt;&lt;amUtland /&gt;&lt;amEpostadr&gt;postmottak@fmno.no&lt;/amEpostadr&gt;&lt;amRef /&gt;&lt;amJenhet /&gt;&lt;amAvskm /&gt;&lt;amAvskdato /&gt;&lt;amFrist /&gt;&lt;amForsend&gt;D&lt;/amForsend&gt;&lt;amAdmkort&gt;[Ufordelt]&lt;/amAdmkort&gt;&lt;amAdmbet&gt;Ufordelt/sendt tilbake til arkiv&lt;/amAdmbet&gt;&lt;amSbhinit&gt;[Ufordelt]&lt;/amSbhinit&gt;&lt;amSbhnavn&gt;Ikke fordelt til saksbehandler&lt;/amSbhnavn&gt;&lt;amAvsavdok /&gt;&lt;amBesvardok /&gt;&lt;/avsmot&gt;&lt;dokument&gt;&lt;dlRnr&gt;1&lt;/dlRnr&gt;&lt;dlType&gt;H&lt;/dlType&gt;&lt;dbKategori&gt;ND&lt;/dbKategori&gt;&lt;dbTittel&gt;Test MSH Difi&lt;/dbTittel&gt;&lt;dbStatus&gt;F&lt;/dbStatus&gt;&lt;veVariant&gt;A&lt;/veVariant&gt;&lt;veDokformat&gt;RA-PDF&lt;/veDokformat&gt;&lt;fil&gt;&lt;base64&gt;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pPjwvcmRmOmxpPjwvcmRmOlNlcT48L2RjOmNyZWF0b3I+DTwvcmRmOkRlc2NyaXB0aW9uPg08cmRmOkRlc2NyaXB0aW9uIGFib3V0PScnIHhtbG5zPSdodHRwOi8vbnMuYWRvYmUuY29tL3BkZi8xLjMvJyB4bWxuczpwZGY9J2h0dHA6Ly9ucy5hZG9iZS5jb20vcGRmLzEuMy8nPg08cGRmOlByb2R1Y2VyPkFteXVuaSBQREYgQ29udmVydGVyIHZlcnNpb24gNC41LjMuMzwvcGRmOlByb2R1Y2VyPg08L3JkZjpEZXNjcmlwdGlvbj4NPHJkZjpEZXNjcmlwdGlvbiBhYm91dD0nJyB4bWxucz0naHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLycgeG1sbnM6eGFwPSdodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvJz4NPHhhcDpNZXRhZGF0YURhdGU+MjAxNC0xMS0yN1QxNDo1OTozMCswMTowMDwveGFwOk1ldGFkYXRhRGF0ZT4NPHhhcDpDcmVhdGVEYXRlPjIwMTQtMTEtMjdUMTQ6NTk6MzArMDE6MDA8L3hhcDpDcmVhdGVEYXRlPg08L3JkZjpEZXNjcmlwdGlvbj4NPHJkZjpEZXNjcmlwdGlvbiByZGY6YWJvdXQ9JycgeG1sbnM6cGRmYWlkPSdodHRwOi8vd3d3LmFpaW0ub3JnL3BkZmEvbnMvaWQvJz4NPHBkZmFpZDpwYXJ0PjE8L3BkZmFpZDpwYXJ0Pg08cGRmYWlkOmNvbmZvcm1hbmNlPkI8L3BkZmFpZDpjb25mb3JtYW5jZT4NPC9yZGY6RGVzY3JpcHRpb24+DTwvcmRmOlJERj4NPD94cGFja2V0IGVuZD0ncic/Pg0KZW5kc3RyZWFtCmVuZG9iagoyIDAgb2JqDTw8DS9UeXBlIC9DYXRhbG9nDS9QYWdlcyAzIDAgUg0vT3V0cHV0SW50ZW50cyBbMTA1IDAgUl0NL01ldGFkYXRhIDEwNyAwIFINPj4NZW5kb2JqDXhyZWYNMCAxMDgNMDAwMDAwMDAwMCA2NTUzNSBmIA0wMDAwMDAwMDE1IDAwMDAwIG4gDTAwMDAxNzAxOTUgMDAwMDAgbiANMDAwMDE2NjE2NSAwMDAwMCBuIA0wMDAwMDAwMDAwIDAwMDAwIGYgDTAwMDAwMDAwMDAgMDAwMDAgZiANMDAwMDEyOTkwNSAwMDAwMCBuIA0wMDAwMDAwMTU3IDAwMDAwIG4gDTAwMDAwMTE3MzAgMDAwMDAgbiANMDAwMDAxMTc1MSAwMDAwMCBuIA0wMDAwMDEzMTg0IDAwMDAwIG4gDTAwMDAwMTMyMDUgMDAwMDAgbiANMDAwMDAxNDIxNCAwMDAwMCBuIA0wMDAwMDE0MjM0IDAwMDAwIG4gDTAwMDAwMTU2MzcgMDAwMDAgbiANMDAwMDAxNTY1OCAwMDAwMCBuIA0wMDAwMDE3NTc2IDAwMDAwIG4gDTAwMDAwMTc1OTcgMDAwMDAgbiANMDAwMDAxODQ4OSAwMDAwMCBuIA0wMDAwMDE4NTA5IDAwMDAwIG4gDTAwMDAwMTkyMjIgMDAwMDAgbiANMDAwMDAxOTI0MiAwMDAwMCBuIA0wMDAwMDIwMTU2IDAwMDAwIG4gDTAwMDAwMjAxNzYgMDAwMDAgbiANMDAwMDA2MzIwNCAwMDAwMCBuIA0wMDAwMDYzMjI2IDAwMDAwIG4gDTAwMDAwNjQ0MzUgMDAwMDAgbiANMDAwMDA2NDQ1NiAwMDAwMCBuIA0wMDAwMDY1MzU1IDAwMDAwIG4gDTAwMDAwNjUzNzUgMDAwMDAgbiANMDAwMDA2NjYzMiAwMDAwMCBuIA0wMDAwMDY2NjUzIDAwMDAwIG4gDTAwMDAwNjc1NjcgMDAwMDAgbiANMDAwMDA2NzU4NyAwMDAwMCBuIA0wMDAwMDY4NzY5IDAwMDAwIG4gDTAwMDAwNjg3ODkgMDAwMDAgbiANMDAwMDA2ODkxNCAwMDAwMCBuIA0wMDAwMDY4OTMzIDAwMDAwIG4gDTAwMDAwNzAyMDQgMDAwMDAgbiANMDAwMDA3MDIyNSAwMDAwMCBuIA0wMDAwMDcwMzUwIDAwMDAwIG4gDTAwMDAwNzAzNjkgMDAwMDAgbiANMDAwMDA3MTY0MCAwMDAwMCBuIA0wMDAwMDcxNjYxIDAwMDAwIG4gDTAwMDAwNzE3ODUgMDAwMDAgbiANMDAwMDA3MTgwNCAwMDAwMCBuIA0wMDAwMDczMDI4IDAwMDAwIG4gDTAwMDAwNzMwNDkgMDAwMDAgbiANMDAwMDEyNDE3MiAwMDAwMCBuIA0wMDAwMTMwNjI1IDAwMDAwIG4gDTAwMDAxMjQxOTQgMDAwMDAgbiANMDAwMDEyNDQ1MyAwMDAwMCBuIA0wMDAwMTI0NDcyIDAwMDAwIG4gDTAwMDAxMjQ2MTcgMDAwMDAgbiANMDAwMDEyNDYzNiAwMDAwMCBuIA0wMDAwMTI0ODk1IDAwMDAwIG4gDTAwMDAxMjQ5MTQgMDAwMDAgbiANMDAwMDEyNTI0NiAwMDAwMCBuIA0wMDAwMTI1MjY2IDAwMDAwIG4gDTAwMDAxMjU1MjUgMDAwMDAgbiANMDAwMDEyNTU0NCAwMDAwMCBuIA0wMDAwMTI1NjkwIDAwMDAwIG4gDTAwMDAxMjU3MDkgMDAwMDAgbiANMDAwMDEyNTk2OCAwMDAwMCBuIA0wMDAwMTI1OTg3IDAwMDAwIG4gDTAwMDAxMjYzMjMgMDAwMDAgbiANMDAwMDEyNjM0MyAwMDAwMCBuIA0wMDAwMTI2NjAyIDAwMDAwIG4gDTAwMDAxMjY2MjEgMDAwMDAgbiANMDAwMDEyNjc2NyAwMDAwMCBuIA0wMDAwMTI2Nzg2IDAwMDAwIG4gDTAwMDAxMjcwNDUgMDAwMDAgbiANMDAwMDEyNzA2NCAwMDAwMCBuIA0wMDAwMTI3Mzg3IDAwMDAwIG4gDTAwMDAxMjc0MDcgMDAwMDAgbiANMDAwMDEyNzY2NiAwMDAwMCBuIA0wMDAwMTI3Njg1IDAwMDAwIG4gDTAwMDAxMjc4MjggMDAwMDAgbiANMDAwMDEyNzg0NyAwMDAwMCBuIA0wMDAwMTI4MTA2IDAwMDAwIG4gDTAwMDAxMjgxMjUgMDAwMDAgbiANMDAwMDEyODQxOCAwMDAwMCBuIA0wMDAwMTI4NDM4IDAwMDAwIG4gDTAwMDAxMjg2OTcgMDAwMDAgbiANMDAwMDEyODcxNiAwMDAwMCBuIA0wMDAwMTI4ODYwIDAwMDAwIG4gDTAwMDAxMjg4NzkgMDAwMDAgbiANMDAwMDEyOTEzOCAwMDAwMCBuIA0wMDAwMTI5MTU3IDAwMDAwIG4gDTAwMDAxMjk4ODUgMDAwMDAgbiANMDAwMDE1NTg3NCAwMDAwMCBuIA0wMDAwMTMxMzQwIDAwMDAwIG4gDTAwMDAxMzA3NzMgMDAwMDAgbiANMDAwMDEzMTc5MyAwMDAwMCBuIA0wMDAwMTMyMDg2IDAwMDAwIG4gDTAwMDAxMzIxNjcgMDAwMDAgbiANMDAwMDE1NTgzMCAwMDAwMCBuIA0wMDAwMTU1ODUyIDAwMDAwIG4gDTAwMDAxNTY0MDIgMDAwMDAgbiANMDAwMDE1NjAyNyAwMDAwMCBuIA0wMDAwMTU2Njc2IDAwMDAwIG4gDTAwMDAxNTY5NzggMDAwMDAgbiANMDAwMDE1NzA1OCAwMDAwMCBuIA0wMDAwMTY2MTIwIDAwMDAwIG4gDTAwMDAxNjYxNDIgMDAwMDAgbiANMDAwMDE2NjIyMyAwMDAwMCBuIA0wMDAwMTY2MzU5IDAwMDAwIG4gDTAwMDAxNjkwMzYgMDAwMDAgbiANdHJhaWxlcg08PA0vU2l6ZSAxMDgNL1Jvb3QgMiAwIFINL0luZm8gMSAwIFINL0lEIFs8MkNDQUNBM0JEODA1Rjk0OUI2MTFENzRGMzJENEQxRUU+PDJDQ0FDQTNCRDgwNUY5NDlCNjExRDc0RjMyRDREMUVFPl0NPj4Nc3RhcnR4cmVmDTE3MDI4Nw0lJUVPRg0=&lt;/base64&gt;&lt;/fil&gt;&lt;veFilnavn /&gt;&lt;veMimeType /&gt;&lt;/dokument&gt;&lt;/journpost&gt;&lt;noarksak xmlns=&quot;&quot;&gt;&lt;saId&gt;68286&lt;/saId&gt;&lt;saSaar&gt;2014&lt;/saSaar&gt;&lt;saSeknr&gt;2703&lt;/saSeknr&gt;&lt;saPapir&gt;0&lt;/saPapir&gt;&lt;saDato&gt;2014-11-27&lt;/saDato&gt;&lt;saTittel&gt;Test Knutepunkt herokuapp&lt;/saTittel&gt;&lt;saU1&gt;0&lt;/saU1&gt;&lt;saStatus&gt;B&lt;/saStatus&gt;&lt;saArkdel&gt;EARKIV1&lt;/saArkdel&gt;&lt;saType /&gt;&lt;saJenhet&gt;SENTRAL&lt;/saJenhet&gt;&lt;saTgkode /&gt;&lt;saUoff /&gt;&lt;saBevtid /&gt;&lt;saKasskode /&gt;&lt;saKassdato /&gt;&lt;saProsjekt /&gt;&lt;saOfftittel&gt;Test Knutepunkt herokuapp&lt;/saOfftittel&gt;&lt;saAdmkort&gt;FM-ADMA&lt;/saAdmkort&gt;&lt;saAdmbet&gt;Administrasjon&lt;/saAdmbet&gt;&lt;saAnsvinit&gt;JPS&lt;/saAnsvinit&gt;&lt;saAnsvnavn&gt;John Petter Svedal&lt;/saAnsvnavn&gt;&lt;saTggruppnavn /&gt;&lt;/noarksak&gt;&lt;/Melding&gt;</data>\n" +
            "                                    </default>\n" +
            "                                </com.sun.org.apache.xerces.internal.dom.CharacterDataImpl>\n" +
            "                            </firstChild>\n" +
            "                            <ownerDocument class=\"com.sun.org.apache.xerces.internal.dom.DocumentImpl\" reference=\"../../../com.sun.org.apache.xerces.internal.dom.NodeImpl/default/ownerNode\"/>\n" +
            "                        </default>\n" +
            "                    </com.sun.org.apache.xerces.internal.dom.ParentNode>\n" +
            "                    <com.sun.org.apache.xerces.internal.dom.ElementImpl>\n" +
            "                        <default>\n" +
            "                            <name>payload</name>\n" +
            "                        </default>\n" +
            "                    </com.sun.org.apache.xerces.internal.dom.ElementImpl>\n" +
            "                    <com.sun.org.apache.xerces.internal.dom.ElementNSImpl>\n" +
            "                        <default>\n" +
            "                            <localName>payload</localName>\n" +
            "                        </default>\n" +
            "                    </com.sun.org.apache.xerces.internal.dom.ElementNSImpl>\n" +
            "                </payload>\n" +
            "            </typ:PutMessageRequest>\n" +
            "        </soapenv:Body>\n" +
            "    </soapenv:Envelope>";

    private String p360Style = "&lt;Melding xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.arkivverket.no/Noark4-1-WS-WD/types\"&gt;\n" +
            "  &lt;journpost xmlns=\"\"&gt;\n" +
            "    &lt;jpId&gt;210707&lt;/jpId&gt;\n" +
            "    &lt;jpJaar&gt;2015&lt;/jpJaar&gt;\n" +
            "    &lt;jpSeknr&gt;47&lt;/jpSeknr&gt;\n" +
            "    &lt;jpJpostnr&gt;7&lt;/jpJpostnr&gt;\n" +
            "    &lt;jpJdato&gt;0001-01-01&lt;/jpJdato&gt;\n" +
            "    &lt;jpNdoktype&gt;U&lt;/jpNdoktype&gt;\n" +
            "    &lt;jpDokdato&gt;2015-09-11&lt;/jpDokdato&gt;\n" +
            "    &lt;jpStatus&gt;R&lt;/jpStatus&gt;\n" +
            "    &lt;jpInnhold&gt;Testdokument 7&lt;/jpInnhold&gt;\n" +
            "    &lt;jpForfdato /&gt;\n" +
            "    &lt;jpTgkode&gt;U&lt;/jpTgkode&gt;\n" +
            "    &lt;jpAgdato /&gt;\n" +
            "    &lt;jpAntved /&gt;\n" +
            "    &lt;jpSaar&gt;2015&lt;/jpSaar&gt;\n" +
            "    &lt;jpSaseknr&gt;20&lt;/jpSaseknr&gt;\n" +
            "    &lt;jpOffinnhold&gt;Testdokument 7&lt;/jpOffinnhold&gt;\n" +
            "    &lt;jpTggruppnavn&gt;Alle&lt;/jpTggruppnavn&gt;\n" +
            "    &lt;avsmot&gt;\n" +
            "      &lt;amIhtype&gt;0&lt;/amIhtype&gt;\n" +
            "      &lt;amNavn&gt;Saksbehandler Testbruker7&lt;/amNavn&gt;\n" +
            "      &lt;amAdresse&gt;Postboks 8115 Dep.&lt;/amAdresse&gt;\n" +
            "      &lt;amPostnr&gt;0032&lt;/amPostnr&gt;\n" +
            "      &lt;amPoststed&gt;OSLO&lt;/amPoststed&gt;\n" +
            "      &lt;amUtland&gt;Norge&lt;/amUtland&gt;\n" +
            "      &lt;amEpostadr&gt;sa-user.test2@difi.no&lt;/amEpostadr&gt;\n" +
            "    &lt;/avsmot&gt;\n" +
            "    &lt;avsmot&gt;\n" +
            "      &lt;amOrgnr&gt;974720760&lt;/amOrgnr&gt;\n" +
            "      &lt;amIhtype&gt;1&lt;/amIhtype&gt;\n" +
            "      &lt;amNavn&gt;EduTestOrg 1&lt;/amNavn&gt;\n" +
            "    &lt;/avsmot&gt;\n" +
            "    &lt;dokument&gt;\n" +
            "      &lt;dlRnr&gt;1&lt;/dlRnr&gt;\n" +
            "      &lt;dlType&gt;H&lt;/dlType&gt;\n" +
            "      &lt;dbTittel&gt;Testdokument 7&lt;/dbTittel&gt;\n" +
            "      &lt;dbStatus&gt;B&lt;/dbStatus&gt;\n" +
            "      &lt;veVariant&gt;P&lt;/veVariant&gt;\n" +
            "      &lt;veDokformat&gt;DOCX&lt;/veDokformat&gt;\n" +
            "      &lt;veFilnavn&gt;Testdokument 7.DOCX&lt;/veFilnavn&gt;\n" +
            "      &lt;veMimeType&gt;application/vnd.openxmlformats-officedocument.wordprocessingml.document&lt;/veMimeType&gt;\n" +
            "    &lt;/dokument&gt;\n" +
            "  &lt;/journpost&gt;\n" +
            "  &lt;noarksak xmlns=\"\"&gt;\n" +
            "    &lt;saId&gt;15/00020&lt;/saId&gt;\n" +
            "    &lt;saSaar&gt;2015&lt;/saSaar&gt;\n" +
            "    &lt;saSeknr&gt;20&lt;/saSeknr&gt;\n" +
            "    &lt;saPapir&gt;0&lt;/saPapir&gt;\n" +
            "    &lt;saDato&gt;2015-09-01&lt;/saDato&gt;\n" +
            "    &lt;saTittel&gt;BEST/EDU testsak&lt;/saTittel&gt;\n" +
            "    &lt;saStatus&gt;R&lt;/saStatus&gt;\n" +
            "    &lt;saArkdel&gt;Sakarkiv 2013&lt;/saArkdel&gt;\n" +
            "    &lt;saType&gt;Sak&lt;/saType&gt;\n" +
            "    &lt;saJenhet&gt;Oslo&lt;/saJenhet&gt;\n" +
            "    &lt;saTgkode&gt;U&lt;/saTgkode&gt;\n" +
            "    &lt;saBevtid /&gt;\n" +
            "    &lt;saKasskode&gt;B&lt;/saKasskode&gt;\n" +
            "    &lt;saOfftittel&gt;BEST/EDU testsak&lt;/saOfftittel&gt;\n" +
            "    &lt;saAdmkort&gt;202286&lt;/saAdmkort&gt;\n" +
            "    &lt;saAdmbet&gt;Seksjon for test 1&lt;/saAdmbet&gt;\n" +
            "    &lt;saAnsvinit&gt;difi\\sa-user-test2&lt;/saAnsvinit&gt;\n" +
            "    &lt;saAnsvnavn&gt;Saksbehandler Testbruker7&lt;/saAnsvnavn&gt;\n" +
            "    &lt;saTggruppnavn&gt;Alle&lt;/saTggruppnavn&gt;\n" +
            "    &lt;sakspart&gt;\n" +
            "      &lt;spId&gt;0&lt;/spId&gt;\n" +
            "    &lt;/sakspart&gt;\n" +
            "  &lt;/noarksak&gt;\n" +
            "&lt;/Melding&gt;";


    /**
     * The AppReceiptStrategy should only log the message to the eventlog,
     * and return an OK response type
     */
    @Test
    public void shouldHandleP360StylePayload() {

        PutMessageContext ctx = new PutMessageContext(Mockito.mock(EventLog.class), Mockito.mock(MessageSender.class));
        BestEDUPutMessageStrategy strategy = new BestEDUPutMessageStrategy(ctx);

        PutMessageRequestType request = new PutMessageRequestType();
        request.setPayload(p360Style);

        PutMessageResponseType response = strategy.putMessage(request);

        assertTrue(response.getResult() != null);
        verify(ctx.getMessageSender(), times(1)).sendMessage(any(PutMessageRequestType.class));
    }

    @Test
    public void testShouldHandleEPhortePaload() throws ParserConfigurationException {
        MessageSender messageSenderMock = Mockito.mock(MessageSender.class);
        PutMessageContext ctx = new PutMessageContext(Mockito.mock(EventLog.class), messageSenderMock);
        BestEDUPutMessageStrategy strategy = new BestEDUPutMessageStrategy(ctx);
        Document document;
        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            document = documentBuilder.parse(new InputSource(new ByteArrayInputStream(ePhorteStyle.getBytes())));
        } catch (SAXException | IOException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
        PutMessageRequestType req = new PutMessageRequestType();
        req.setPayload(document);
        strategy.putMessage(req);
        verify(ctx.getMessageSender(), times(1)).sendMessage(any(PutMessageRequestType.class));
    }
}