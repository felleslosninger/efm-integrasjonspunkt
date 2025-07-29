Feature: Sending a Next Move Digital DPV message

  Background:
    Given a "GET" request to "http://localhost:9099/identifier/17912099997?securityLevel=3" will respond with status "200" and the following "application/json" in "/restmocks/identifier/17912099997.json"
    And a "GET" request to "http://localhost:9099/identifier/17912099997/process/urn:no:difi:profile:digitalpost:info:ver1.0?conversationId=97efbd4c-413d-4e2c-bbc5-257ef4a61212" will respond with status "200" and the following "application/json" in "/restmocks/identifier/17912099997-info.json"
    And a "GET" request to "http://localhost:9099/identifier/974720760" will respond with status "200" and the following "application/json" in "/restmocks/identifier/974720760.json"
    And a "GET" request to "http://localhost:9099/info/910077473" will respond with status "200" and the following "application/json" in "/restmocks/info/910077473.json"
    And a "GET" request to "http://localhost:9099/virksert/910077473" will respond with status "200" and the following "text/plain" in "/restmocks/virksert/910077473"
    And a CorrespondenceClient request to "/correspondence/api/v1/correspondence/upload" will respond with the following payload:
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

  Scenario: As a user I want to send a Digital DPV message
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
                        "identifier": "urn:no:difi:profile:digitalpost:info:ver1.0",
                        "instanceIdentifier": "97efbd4c-413d-4e2c-bbc5-257ef4a61212",
                        "type": "ConversationId"
                    }
                ]
            },
            "documentIdentification": {
                "creationDateAndTime": "2019-03-25T11:35:00+01:00",
                "instanceIdentifier": "ff88849c-e281-4809-8555-7cd54952b922",
                "standard": "urn:no:difi:digitalpost:xsd:digital::digital_dpv",
                "type": "digital_dpv",
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
        "digital_dpv": {
            "tittel": "foo",
            "sammendrag": "bar",
            "innhold": "foobar"
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
    <InsertCorrespondenceV2 >
        <SystemUserCode>stuntman</SystemUserCode>
        <ExternalShipmentReference>ff88849c-e281-4809-8555-7cd54952b922</ExternalShipmentReference>
        <Correspondence>
            <ServiceCode>4255</ServiceCode>
            <ServiceEdition>10</ServiceEdition>
            <Reportee>17912099997</Reportee>
            <Content>
                <LanguageCode>1044</LanguageCode>
                <MessageTitle>foo</MessageTitle>
                <MessageSummary>bar</MessageSummary>
                <MessageBody>foobar</MessageBody>
                <Attachments>
                    <BinaryAttachments>
                        <BinaryAttachmentV2>
                            <FunctionType>Unspecified</FunctionType>
                            <FileName>test.txt</FileName>
                            <Name>Test</Name>
                            <Encrypted>false</Encrypted>
                            <Data></Data>
                            <SendersReference>AttachmentReference_as123452</SendersReference>
                        </BinaryAttachmentV2>
                    </BinaryAttachments>
                </Attachments>
            </Content>
        <VisibleDateTime>2019-03-25T12:38:23.000+01:00</VisibleDateTime>
        <AllowSystemDeleteDateTime>2019-03-25T12:43:23.000+01:00</AllowSystemDeleteDateTime>
        <DueDateTime>2019-04-01T12:38:23.000+01:00</DueDateTime>
        <Notifications>
            <Notification>
                <FromAddress>no-reply@altinn.no</FromAddress>
                <ShipmentDateTime>2019-03-25T12:43:23.000+01:00</ShipmentDateTime>
                <LanguageCode>1044</LanguageCode>
                <NotificationType>VarselDPVMedRevarsel</NotificationType>
                <TextTokens>
                    <TextToken>
                        <TokenNum>1</TokenNum>
                        <TokenValue>$reporteeName$: Du har mottatt en melding fra TEST - C4.</TokenValue>
                    </TextToken>
                </TextTokens>
                <ReceiverEndPoints>
                    <ReceiverEndPoint>
                        <TransportType>Both</TransportType>
                    </ReceiverEndPoint>
                </ReceiverEndPoints>
            </Notification>
        </Notifications>
        <AllowForwarding>true</AllowForwarding>
        <MessageSender>TEST - C4</MessageSender>
        </Correspondence>
    </InsertCorrespondenceV2>
    """
    And the sent message contains the following files:
      | filename | content type |
      | test.txt | text/plain   |
