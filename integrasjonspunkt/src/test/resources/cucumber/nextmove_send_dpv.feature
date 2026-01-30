Feature: Sending a Next Move DPV message

  Background:
    Given a "GET" request to "http://localhost:9099/identifier/910075946" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910075946.json"
    And a "GET" request to "http://localhost:9099/identifier/910075946/process/urn:no:difi:profile:arkivmelding:administrasjon:ver1.0?securityLevel=3&conversationId=45efbd4c-413d-4e2c-bbc5-257ef4a65a91" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910075946-administrasjon.json"
    And a "GET" request to "http://localhost:9099/identifier/910075946/process/urn:no:difi:profile:arkivmelding:administrasjon:ver1.0?conversationId=45efbd4c-413d-4e2c-bbc5-257ef4a65a91" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910075946-administrasjon.json"
    And a "GET" request to "http://localhost:9099/identifier/910077473" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910077473.json"
    And a "GET" request to "http://localhost:9099/info/974720760" will respond with status "200" and the following "application/json" in "/restmocks/info/974720760.json"
    And a "GET" request to "http://localhost:9099/info/910077473" will respond with status "200" and the following "application/json" in "/restmocks/info/910077473.json"
    And a "GET" request to "http://localhost:9099/virksert/910077473" will respond with status "200" and the following "text/plain" in "/restmocks/virksert/910077473"
    And a CorrespondenceClient request to "/correspondence/api/v1/correspondence/upload" will respond with the following payload:
    """
        {
          "correspondences" : [ {
            "correspondenceId" : "0198408f-979c-75e0-b23e-aef6e907015f",
            "status" : "Initialized",
            "recipient" : "urn:altinn:organization:identifier-no:314244370",
            "notifications" : null
          } ],
          "attachmentIds" : [ "0198408f-9736-7048-a634-39138f4ccbdd", "0198408f-973c-7ba2-a161-40dec03900fb" ]
        }
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
                                "expectedResponseDateTime": "2019-05-10T00:31:52+01:00"
                            }
                        ],
                        "identifier": "urn:no:difi:profile:arkivmelding:administrasjon:ver1.0",
                        "instanceIdentifier": "45efbd4c-413d-4e2c-bbc5-257ef4a65a91",
                        "type": "ConversationId"
                    }
                ]
            },
            "documentIdentification": {
                "creationDateAndTime": "2019-03-25T11:35:00+01:00",
                "instanceIdentifier": "abc8849c-e281-4809-8555-7cd54952b926",
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
          "hoveddokument": "test.txt"
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
    {
        "correspondence.content.messageTitle" : "Nye lysrør",
        "correspondence.notification.notificationChannel" : "EmailAndSms",
        "correspondence.notification.reminderSmsBody" : "$correspondenceRecipientName$: Du har mottatt en melding fra TEST - C4.",
        "correspondence.sendersReference" : "abc8849c-e281-4809-8555-7cd54952b926",
        "correspondence.notification.reminderEmailContentType" : "Plain",
        "correspondence.notification.smsBody" : "$correspondenceRecipientName$: Du har mottatt en melding fra TEST - C4.",
        "correspondence.notification.reminderEmailBody" : "$correspondenceRecipientName$: Du har mottatt en melding fra TEST - C4.",
        "correspondence.sender" : "0192:910077473",
        "correspondence.requestedPublishTime" : "2019-03-25T12:38:23+01:00",
        "correspondence.notification.requestedSendTime" : "2019-03-25T12:43:23+01:00",
        "correspondence.isConfirmationNeeded" : "false",
        "correspondence.content.language" : "nb",
        "correspondence.notification.emailContentType" : "Plain",
        "correspondence.messageSender" : "TEST - C4",
        "correspondence.notification.emailBody" : "$correspondenceRecipientName$: Du har mottatt en melding fra TEST - C4.",
        "correspondence.notification.reminderNotificationChannel" : "EmailAndSms",
        "correspondence.dueDateTime" : "2019-04-01T12:38:23+01:00",
        "correspondence.content.attachments[0].sendersReference" : "AttachmentReference_as123452",
        "correspondence.notification.sendReminder" : "true",
        "recipients[0]" : "urn:altinn:organization:identifier-no:910075946",
        "correspondence.content.attachments[0].isEncrypted" : "false",
        "correspondence.isConfidential" : "false",
        "correspondence.content.messageSummary" : "Nye lysrør",
        "correspondence.content.attachments[0].fileName" : "test.txt",
        "correspondence.content.messageBody" : "Nye lysrør",
        "correspondence.content.attachments[0].displayName" : "Test",
        "correspondence.notification.notificationTemplate" : "CustomMessage",
        "correspondence.notification.emailSubject" : "Melding mottatt i Altinn",
        "correspondence.notification.reminderEmailSubject" : "Melding mottatt i Altinn"
    }
    """
    And the sent message contains the following files:
      | filename         | content type |
      | test.txt         | text/plain   |
    And the content of the file named "test.txt" is:
    """
    Testing 1 2 3
    """
