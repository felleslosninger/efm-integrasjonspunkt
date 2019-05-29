Feature: Sending a Next Move Digital DPV message

  Background:
    Given a "GET" request to "http://localhost:9099/identifier/09118532323?notification=obligated" will respond with status "200" and the following "application/json" in "/restmocks/identifier/09118532323.json"
    And a "GET" request to "http://localhost:9099/identifier/974720760?notification=obligated" will respond with status "200" and the following "application/json" in "/restmocks/identifier/974720760.json"
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

  Scenario: As a user I want to send a DPI message
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
                        "identifier": "urn:no:difi:profile:digitalpost:info:ver1.0",
                        "instanceIdentifier": "97efbd4c-413d-4e2c-bbc5-257ef4a61212",
                        "type": "ConversationId"
                    }
                ]
            },
            "documentIdentification": {
                "creationDateAndTime": "2019-03-25T11:35:00Z",
                "instanceIdentifier": "ff88849c-e281-4809-8555-7cd54952b916",
                "standard": "urn:no:difi:digitalpost:xsd:digital::digital_dpv",
                "type": "digital_dpv",
                "typeVersion": "2.0"
            },
            "headerVersion": "1.0",
            "receiver": [
                {
                    "identifier": {
                        "authority": "iso6523-actorid-upis",
                        "value": "09118532323"
                    }
                }
            ],
            "sender": [
                {
                    "identifier": {
                        "authority": "iso6523-actorid-upis",
                        "value": "0192:974720760"
                    }
                }
            ]
        },
        "digital_dpv": {
            "title": "foo",
            "summary": "bar",
            "body": "foobar"
        }
    }
    """
    And the response status is "OK"
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
        <altinn9:ExternalShipmentReference>97efbd4c-413d-4e2c-bbc5-257ef4a61212</altinn9:ExternalShipmentReference>
        <altinn9:Correspondence>
            <altinn10:ServiceCode>4255</altinn10:ServiceCode>
            <altinn10:ServiceEdition>10</altinn10:ServiceEdition>
            <altinn10:Reportee>09118532323</altinn10:Reportee>
            <altinn10:Content>
                <altinn10:LanguageCode>1044</altinn10:LanguageCode>
                <altinn10:MessageTitle>foo</altinn10:MessageTitle>
                <altinn10:MessageSummary>bar</altinn10:MessageSummary>
                <altinn10:MessageBody>foobar</altinn10:MessageBody>
                <altinn10:Attachments>
                    <altinn10:BinaryAttachments>
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
        <altinn10:VisibleDateTime>2019-03-25T11:38:23.000Z</altinn10:VisibleDateTime>
        <altinn10:AllowSystemDeleteDateTime>2019-03-25T11:43:23.000Z</altinn10:AllowSystemDeleteDateTime>
        <altinn10:DueDateTime>2019-04-01T11:38:23.000Z</altinn10:DueDateTime>
        <altinn10:Notifications>
            <altinn13:Notification>
                <altinn13:FromAddress>no-reply@altinn.no</altinn13:FromAddress>
                <altinn13:ShipmentDateTime>2019-03-25T11:43:23.000Z</altinn13:ShipmentDateTime>
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
      | filename | content type |
      | test.txt | text/plain   |
