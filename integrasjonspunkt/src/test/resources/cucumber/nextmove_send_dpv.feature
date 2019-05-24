Feature: Sending a Next Move DPV message

  Background:
    Given a "GET" request to "http://localhost:9099/identifier/910075946?notification=obligated" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910075946.json"
    And a "GET" request to "http://localhost:9099/identifier/910077473?notification=obligated" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910077473.json"
    And a "GET" request to "http://localhost:9099/identifier/974720760" will respond with status "200" and the following "application/json" in "/restmocks/identifier/974720760.json"
    And a SOAP request to "http://localhost:9876/ServiceEngineExternal/CorrespondenceAgencyExternal.svc" will respond with the following payload:
    """
     <InsertCorrespondenceV2Response xmlns="http://www.altinn.no/services/ServiceEngine/Correspondence/2009/10">
        <InsertCorrespondenceV2Result xmlns:b="http://schemas.altinn.no/services/Intermediary/Receipt/2009/10" xmlns:i="http://www.w3.org/2001/XMLSchema-instance">
           <b:LastChanged>${created}</b:LastChanged>
           <b:ParentReceiptId>0</b:ParentReceiptId>
           <b:ReceiptHistory>
           ${created} - OK - Correspondence Saved Successfully
           </b:ReceiptHistory>
           <!--  DATE NOW Apr 19 2018  9:01AM - OK - Correspondence Saved Successfully  -->
           <b:ReceiptId>${receiptId}</b:ReceiptId>
           <b:ReceiptStatusCode>OK</b:ReceiptStatusCode>
           <b:ReceiptText>Correspondence Saved Successfully</b:ReceiptText>
           <b:ReceiptTypeName>Correspondence</b:ReceiptTypeName>
           <b:References>
              <b:Reference>
                 <b:ReferenceTypeName>ExternalShipmentReference</b:ReferenceTypeName>
                 <b:ReferenceValue>${sendersReference}</b:ReferenceValue>
              </b:Reference>
              <b:Reference>

                 <b:ReferenceTypeName>OwnerPartyReference</b:ReferenceTypeName>
                 <!-- Reportee hentes fra ns1:Reportee, skal være et orgnummer.-->
                 <b:ReferenceValue>${reportee}</b:ReferenceValue>
              </b:Reference>
           </b:References>
           <b:SubReceipts i:nil="true"/>
        </InsertCorrespondenceV2Result>
     </InsertCorrespondenceV2Response>
    """

  Scenario: As a user I want to send a DPV message
    Given I POST the following message:
    """
    {
        "standardBusinessDocumentHeader": {
            "businessScope": {
                "scope": [
                    {
                        "scopeInformation": [
                            {
                                "expectedResponseDateTime": "2019-05-10T00:31:52Z"
                            }
                        ],
                        "identifier": "urn:no:difi:profile:arkivmelding:administrasjon:ver1.0",
                        "instanceIdentifier": "45efbd4c-413d-4e2c-bbc5-257ef4a65a91",
                        "type": "ConversationId"
                    }
                ]
            },
            "documentIdentification": {
                "creationDateAndTime": "2019-04-11T15:29:58.753+02:00",
                "instanceIdentifier": "abc8849c-e281-4809-8555-7cd54952b916",
                "standard": "urn:no:difi:arkivmelding:xsd::arkivmelding",
                "type": "arkivmelding",
                "typeVersion": "2.0"
            },
            "headerVersion": "1.0",
            "receiver": [
                {
                    "identifier": {
                        "authority": "iso6523-actorid-upis",
                        "value": "0192:910075946"
                    }
                }
            ],
            "sender": [
                {
                    "identifier": {
                        "authority": "iso6523-actorid-upis",
                        "value": "0192:910077473"
                    }
                }
            ]
        },
        "arkivmelding": {
          "sikkerhetsnivaa": 3,
          "primaerDokumentNavn": "test.txt"
        }
    }
    """
    And the response status is "OK"
    And I upload a file named "arkivmelding.xml" with mimetype "text/xml" and title "Arkivmelding" with the following body:
    """
    <?xml version="1.0" encoding="utf-8"?>
    <arkivmelding xmlns="http://www.arkivverket.no/standarder/noark5/arkivmelding" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.arkivverket.no/standarder/noark5/arkivmelding arkivmelding.xsd">
        <system>LandLord</system>
        <meldingId>3380ed76-5d4c-43e7-aa70-8ed8d97e4835</meldingId>
        <tidspunkt>2017-05-23T12:46:00</tidspunkt>
        <antallFiler>1</antallFiler>

        <mappe xsi:type="saksmappe">
            <systemID>43fbe161-7aac-4c9f-a888-d8167aab4144</systemID>
            <tittel>Nye lysrør Hauketo Skole</tittel>
            <opprettetDato>2017-06-01T10:10:12.000+01:00</opprettetDato>
            <opprettetAv/>
            <klassifikasjon>
                <referanseKlassifikasjonssystem>Funksjoner</referanseKlassifikasjonssystem>
                <klasseID>vedlikehold av skole</klasseID>
                <tittel>vedlikehold av skole</tittel>
                <opprettetDato>2017-05-23T21:56:12.000+01:00</opprettetDato>
                <opprettetAv>Knut Hansen</opprettetAv>
            </klassifikasjon>
            <klassifikasjon>
                <referanseKlassifikasjonssystem>Objekter</referanseKlassifikasjonssystem>
                <klasseID>20500</klasseID>
                <tittel>Hauketo Skole</tittel>
                <opprettetDato>2017-05-23T21:56:12.000+01:00</opprettetDato>
                <opprettetAv>Knut Hansen</opprettetAv>
            </klassifikasjon>
            <basisregistrering xsi:type="journalpost">
                <systemID>430a6710-a3d4-4863-8bd0-5eb1021bee45</systemID>
                <opprettetDato>2012-02-17T21:56:12.000+01:00</opprettetDato>
                <opprettetAv>LandLord</opprettetAv>
                <arkivertDato>2012-02-17T21:56:12.000+01:00</arkivertDato>
                <arkivertAv>LandLord</arkivertAv>
                <referanseForelderMappe>43fbe161-7aac-4c9f-a888-d8167aab4144</referanseForelderMappe>
                <dokumentbeskrivelse>
                    <systemID>3e518e5b-a361-42c7-8668-bcbb9eecf18d</systemID>
                    <dokumenttype>Bestilling</dokumenttype>
                    <dokumentstatus>Dokumentet er ferdigstilt</dokumentstatus>
                    <tittel>Bestilling - nye lysrør</tittel>
                    <opprettetDato>2012-02-17T21:56:12.000+01:00</opprettetDato>
                    <opprettetAv>Landlord</opprettetAv>
                    <tilknyttetRegistreringSom>Hoveddokument</tilknyttetRegistreringSom>
                    <dokumentnummer>1</dokumentnummer>
                    <tilknyttetDato>2012-02-17T21:56:12.000+01:00</tilknyttetDato>
                    <tilknyttetAv>Landlord</tilknyttetAv>
                    <dokumentobjekt>
                        <versjonsnummer>1</versjonsnummer>
                        <variantformat>Produksjonsformat</variantformat>
                        <opprettetDato>2012-02-17T21:56:12.000+01:00</opprettetDato>
                        <opprettetAv>Landlord</opprettetAv>
                        <referanseDokumentfil>test.txt</referanseDokumentfil>
                    </dokumentobjekt>
                </dokumentbeskrivelse>
                <tittel>Nye lysrør</tittel>
                <offentligTittel>Nye lysrør</offentligTittel>

                <virksomhetsspesifikkeMetadata>
                    <forvaltningsnummer>20050</forvaltningsnummer>
                    <objektnavn>Hauketo Skole</objektnavn>
                    <eiendom>200501</eiendom>
                    <bygning>2005001</bygning>
                    <bestillingtype>Materiell, elektro</bestillingtype>
                    <rammeavtale>K-123123-elektriker</rammeavtale>
                </virksomhetsspesifikkeMetadata>

                <journalposttype>Utgående dokument</journalposttype>
                <journalstatus>Journalført</journalstatus>
                <journaldato>2017-05-23</journaldato>
                <korrespondansepart>
                    <korrespondanseparttype>Mottaker</korrespondanseparttype>
                    <korrespondansepartNavn>elektrikeren AS, Veien 100, Oslo</korrespondansepartNavn>
                </korrespondansepart>
            </basisregistrering>
            <saksdato>2017-06-01</saksdato>
            <administrativEnhet>Blah</administrativEnhet>
            <saksansvarlig>KNUTKÅRE</saksansvarlig>
            <saksstatus>Avsluttet</saksstatus>
        </mappe>
    </arkivmelding>
    """
    And I upload a file named "test.txt" with mimetype "text/plain" and title "Test" with the following body:
    """
    Testing 1 2 3
    """
    And I send the message
    Then the CorrespondenceAgencyClient is called with the following payload:
    """
    <?xml version="1.0" encoding="UTF-8"?>
    <altinn9:InsertCorrespondenceV2 xmlns:altinn9="http://www.altinn.no/services/ServiceEngine/Correspondence/2009/10"
                                    xmlns:altinn10="http://schemas.altinn.no/services/ServiceEngine/Correspondence/2010/10"
                                    xmlns:altinn11="http://www.altinn.no/services/ServiceEngine/ReporteeElementList/2010/10"
                                    xmlns:altinn12="http://schemas.altinn.no/services/ServiceEngine/Correspondence/2009/10"
                                    xmlns:altinn13="http://schemas.altinn.no/services/ServiceEngine/Notification/2009/10"
                                    xmlns:altinn14="http://schemas.altinn.no/services/ServiceEngine/Correspondence/2016/02"
                                    xmlns:altinn15="http://schemas.altinn.no/services/ServiceEngine/Correspondence/2014/10"
                                    xmlns:altinn16="http://schemas.altinn.no/services/ServiceEngine/Correspondence/2013/11"
                                    xmlns:altinn5="http://schemas.altinn.no/services/Intermediary/Receipt/2009/10"
                                    xmlns:altinn6="http://www.altinn.no/services/ServiceEngine/ReporteeElementList/2009/10"
                                    xmlns:altinn7="http://schemas.altinn.no/services/ServiceEngine/Correspondence/2013/06"
                                    xmlns:altinn8="http://schemas.altinn.no/serviceengine/formsengine/2009/10"
                                    xmlns:soapenv="http://www.w3.org/2003/05/soap-envelope">
        <altinn9:SystemUserCode>stuntman</altinn9:SystemUserCode>
        <altinn9:ExternalShipmentReference>45efbd4c-413d-4e2c-bbc5-257ef4a65a91</altinn9:ExternalShipmentReference>
        <altinn9:Correspondence>
            <altinn10:ServiceCode>4255</altinn10:ServiceCode>
            <altinn10:ServiceEdition>10</altinn10:ServiceEdition>
            <altinn10:Reportee>910075946</altinn10:Reportee>
            <altinn10:Content>
                <altinn10:LanguageCode>1044</altinn10:LanguageCode>
                <altinn10:MessageTitle>Nye lysrør</altinn10:MessageTitle>
                <altinn10:MessageSummary>Nye lysrør</altinn10:MessageSummary>
                <altinn10:MessageBody>Nye lysrør</altinn10:MessageBody>
                <altinn10:Attachments>
                    <altinn10:BinaryAttachments>
                        <altinn11:BinaryAttachmentV2>
                            <altinn11:FunctionType>Unspecified</altinn11:FunctionType>
                            <altinn11:FileName>arkivmelding.xml</altinn11:FileName>
                            <altinn11:Name>Arkivmelding</altinn11:Name>
                            <altinn11:Encrypted>false</altinn11:Encrypted>
                            <altinn11:Data></altinn11:Data>
                            <altinn11:SendersReference>AttachmentReference_as123452</altinn11:SendersReference>
                        </altinn11:BinaryAttachmentV2>
                        <altinn11:BinaryAttachmentV2>
                            <altinn11:FunctionType>Unspecified</altinn11:FunctionType>
                            <altinn11:FileName>test.txt</altinn11:FileName>
                            <altinn11:Name>Test</altinn11:Name>
                            <altinn11:Encrypted>false</altinn11:Encrypted>
                            <altinn11:Data></altinn11:Data>
                            <altinn11:SendersReference>AttachmentReference_as123452</altinn11:SendersReference>
                        </altinn11:BinaryAttachmentV2>
                    </altinn10:BinaryAttachments>
                </altinn10:Attachments>
            </altinn10:Content>
            <altinn10:VisibleDateTime>2019-03-25T12:38:23.000+01:00</altinn10:VisibleDateTime>
            <altinn10:AllowSystemDeleteDateTime>2019-03-25T12:43:23.000+01:00</altinn10:AllowSystemDeleteDateTime>
            <altinn10:DueDateTime>2019-04-01T12:38:23.000+02:00</altinn10:DueDateTime>
            <altinn10:Notifications>
                <altinn13:Notification>
                    <altinn13:FromAddress>no-reply@altinn.no</altinn13:FromAddress>
                    <altinn13:ShipmentDateTime>2019-03-25T12:43:23.000+01:00</altinn13:ShipmentDateTime>
                    <altinn13:LanguageCode>1044</altinn13:LanguageCode>
                    <altinn13:NotificationType>VarselDPVMedRevarsel</altinn13:NotificationType>
                    <altinn13:TextTokens>
                        <altinn13:TextToken>
                            <altinn13:TokenNum>1</altinn13:TokenNum>
                            <altinn13:TokenValue>Du har mottatt en melding fra Test - C2.</altinn13:TokenValue>
                        </altinn13:TextToken>
                    </altinn13:TextTokens>
                    <altinn13:ReceiverEndPoints>
                        <altinn13:ReceiverEndPoint>
                            <altinn13:TransportType>Both</altinn13:TransportType>
                        </altinn13:ReceiverEndPoint>
                    </altinn13:ReceiverEndPoints>
                </altinn13:Notification>
            </altinn10:Notifications>
            <altinn10:AllowForwarding>false</altinn10:AllowForwarding>
            <altinn10:MessageSender>Test - C2</altinn10:MessageSender>
        </altinn9:Correspondence>
    </altinn9:InsertCorrespondenceV2>
    """
    And the sent message contains the following files:
      | filename         | content type |
      | arkivmelding.xml | text/xml     |
      | test.txt         | text/plain   |
    And the content of the file named "arkivmelding.xml" is:
    """
    <?xml version="1.0" encoding="utf-8"?>
    <arkivmelding xmlns="http://www.arkivverket.no/standarder/noark5/arkivmelding" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.arkivverket.no/standarder/noark5/arkivmelding arkivmelding.xsd">
        <system>LandLord</system>
        <meldingId>3380ed76-5d4c-43e7-aa70-8ed8d97e4835</meldingId>
        <tidspunkt>2017-05-23T12:46:00</tidspunkt>
        <antallFiler>1</antallFiler>

        <mappe xsi:type="saksmappe">
            <systemID>43fbe161-7aac-4c9f-a888-d8167aab4144</systemID>
            <tittel>Nye lysrør Hauketo Skole</tittel>
            <opprettetDato>2017-06-01T10:10:12.000+01:00</opprettetDato>
            <opprettetAv/>
            <klassifikasjon>
                <referanseKlassifikasjonssystem>Funksjoner</referanseKlassifikasjonssystem>
                <klasseID>vedlikehold av skole</klasseID>
                <tittel>vedlikehold av skole</tittel>
                <opprettetDato>2017-05-23T21:56:12.000+01:00</opprettetDato>
                <opprettetAv>Knut Hansen</opprettetAv>
            </klassifikasjon>
            <klassifikasjon>
                <referanseKlassifikasjonssystem>Objekter</referanseKlassifikasjonssystem>
                <klasseID>20500</klasseID>
                <tittel>Hauketo Skole</tittel>
                <opprettetDato>2017-05-23T21:56:12.000+01:00</opprettetDato>
                <opprettetAv>Knut Hansen</opprettetAv>
            </klassifikasjon>
            <basisregistrering xsi:type="journalpost">
                <systemID>430a6710-a3d4-4863-8bd0-5eb1021bee45</systemID>
                <opprettetDato>2012-02-17T21:56:12.000+01:00</opprettetDato>
                <opprettetAv>LandLord</opprettetAv>
                <arkivertDato>2012-02-17T21:56:12.000+01:00</arkivertDato>
                <arkivertAv>LandLord</arkivertAv>
                <referanseForelderMappe>43fbe161-7aac-4c9f-a888-d8167aab4144</referanseForelderMappe>
                <dokumentbeskrivelse>
                    <systemID>3e518e5b-a361-42c7-8668-bcbb9eecf18d</systemID>
                    <dokumenttype>Bestilling</dokumenttype>
                    <dokumentstatus>Dokumentet er ferdigstilt</dokumentstatus>
                    <tittel>Bestilling - nye lysrør</tittel>
                    <opprettetDato>2012-02-17T21:56:12.000+01:00</opprettetDato>
                    <opprettetAv>Landlord</opprettetAv>
                    <tilknyttetRegistreringSom>Hoveddokument</tilknyttetRegistreringSom>
                    <dokumentnummer>1</dokumentnummer>
                    <tilknyttetDato>2012-02-17T21:56:12.000+01:00</tilknyttetDato>
                    <tilknyttetAv>Landlord</tilknyttetAv>
                    <dokumentobjekt>
                        <versjonsnummer>1</versjonsnummer>
                        <variantformat>Produksjonsformat</variantformat>
                        <opprettetDato>2012-02-17T21:56:12.000+01:00</opprettetDato>
                        <opprettetAv>Landlord</opprettetAv>
                        <referanseDokumentfil>test.txt</referanseDokumentfil>
                    </dokumentobjekt>
                </dokumentbeskrivelse>
                <tittel>Nye lysrør</tittel>
                <offentligTittel>Nye lysrør</offentligTittel>

                <virksomhetsspesifikkeMetadata>
                    <forvaltningsnummer>20050</forvaltningsnummer>
                    <objektnavn>Hauketo Skole</objektnavn>
                    <eiendom>200501</eiendom>
                    <bygning>2005001</bygning>
                    <bestillingtype>Materiell, elektro</bestillingtype>
                    <rammeavtale>K-123123-elektriker</rammeavtale>
                </virksomhetsspesifikkeMetadata>

                <journalposttype>Utgående dokument</journalposttype>
                <journalstatus>Journalført</journalstatus>
                <journaldato>2017-05-23</journaldato>
                <korrespondansepart>
                    <korrespondanseparttype>Mottaker</korrespondanseparttype>
                    <korrespondansepartNavn>elektrikeren AS, Veien 100, Oslo</korrespondansepartNavn>
                </korrespondansepart>
            </basisregistrering>
            <saksdato>2017-06-01</saksdato>
            <administrativEnhet>Blah</administrativEnhet>
            <saksansvarlig>KNUTKÅRE</saksansvarlig>
            <saksstatus>Avsluttet</saksstatus>
        </mappe>
    </arkivmelding>
    """
    And the content of the file named "test.txt" is:
    """
    Testing 1 2 3
    """