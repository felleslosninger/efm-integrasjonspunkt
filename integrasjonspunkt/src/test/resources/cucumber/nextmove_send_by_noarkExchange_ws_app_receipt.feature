Feature: Sending a BEST/EDU AppReceipt message by the noarkExchange WebService

  Background:

    Given a "GET" request to "http://localhost:9099/identifier/910077473" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910077473.json"
    Given a "GET" request to "http://localhost:9099/identifier/910075918?conversationId=19efbd4c-413d-4e2c-bbc5-257ef4a65b38" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910075918.json"
    Given a "GET" request to "http://localhost:9099/identifier/910075918/process/urn:no:difi:profile:arkivmelding:response:ver1.0?conversationId=19efbd4c-413d-4e2c-bbc5-257ef4a65b38" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910075918-arkivmelding_response.json"
    Given a "GET" request to "http://localhost:9099/virksert/910075918" will respond with status "200" and the following "text/plain" in "/restmocks/virksert/910075918"

  Scenario: As a user I want to send a BEST/EDU AppReceipt message

    # Need to swap sender and receiver due to noarkSystem.type being p360
    Given the sender is "910075918"
    And the receiver is "910077473"
    And the conversationId is "19efbd4c-413d-4e2c-bbc5-257ef4a65b38"
    And the payload is:
    """
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <AppReceipt type="OK" xmlns="http://www.arkivverket.no/Noark/Exchange/types">
      <message code="Recno" xmlns="">
        <text>315890</text>
      </message>
    </AppReceipt>
    """
    And I call the noarkExchange WebService
    Then an upload to Altinn is initiated with:
    """
    <?xml version='1.0' encoding='UTF-8'?>
    <S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
        <S:Body>
            <ns2:InitiateBrokerServiceBasic xmlns="http://schemas.altinn.no/services/ServiceEngine/Broker/2015/06"
                                            xmlns:ns2="http://www.altinn.no/services/ServiceEngine/Broker/2015/06"
                                            xmlns:ns3="http://www.altinn.no/services/2009/10"
                                            xmlns:ns4="http://www.altinn.no/services/common/fault/2009/10"
                                            xmlns:ns5="http://schemas.microsoft.com/2003/10/Serialization/"
                                            xmlns:ns6="http://schemas.altinn.no/services/serviceEntity/2015/06">
                <ns2:systemUserName>testuser</ns2:systemUserName>
                <ns2:systemPassword>testpass</ns2:systemPassword>
                <ns2:brokerServiceInitiation>
                    <Manifest>
                        <ExternalServiceCode>4192</ExternalServiceCode>
                        <ExternalServiceEditionCode>270815</ExternalServiceEditionCode>
                        <ArrayOfFile>
                            <File>
                                <FileName>sbd.zip</FileName>
                            </File>
                        </ArrayOfFile>
                        <Reportee>910077473</Reportee>
                        <SendersReference>ac5efbd4c-413d-4e2c-bbc5-257ef4a65b23</SendersReference>
                    </Manifest>
                    <RecipientList>
                        <Recipient>
                            <PartyNumber>910075918</PartyNumber>
                        </Recipient>
                    </RecipientList>
                </ns2:brokerServiceInitiation>
            </ns2:InitiateBrokerServiceBasic>
        </S:Body>
    </S:Envelope>
    """
    And the sent Altinn ZIP contains the following files:
      | filename       |
      | manifest.xml   |
      | recipients.xml |
      | sbd.json       |
    And the content of the Altinn ZIP file named "manifest.xml" is:
    """
    <?xml version="1.0" encoding="UTF-8"?>
    <BrokerServiceManifest xmlns="http://schema.altinn.no/services/ServiceEngine/Broker/2015/06">
       <ExternalServiceCode>v3888</ExternalServiceCode>
       <ExternalServiceEditionCode>70515</ExternalServiceEditionCode>
       <SendersReference>ac5efbd4c-413d-4e2c-bbc5-257ef4a65b23</SendersReference>
       <Reportee>910077473</Reportee>
       <FileList>
          <File>
             <FileName>sbd.json</FileName>
          </File>
       </FileList>
    </BrokerServiceManifest>
    """
    And the content of the Altinn ZIP file named "recipients.xml" is:
    """
    <?xml version="1.0" encoding="UTF-8"?>
    <BrokerServiceRecipientList xmlns="http://schema.altinn.no/services/ServiceEngine/Broker/2015/06">
       <Recipient>
          <PartyNumber>910075918</PartyNumber>
       </Recipient>
    </BrokerServiceRecipientList>
    """
    And the JSON content of the Altinn ZIP file named "sbd.json" is:
    """
    {
      "standardBusinessDocumentHeader" : {
        "headerVersion" : "1.0",
        "sender" : [ {
          "identifier" : {
            "value" : "0192:910077473",
            "authority" : "iso6523-actorid-upis"
          }
        } ],
        "receiver" : [ {
          "identifier" : {
            "value" : "0192:910075918",
            "authority" : "iso6523-actorid-upis"
          }
        } ],
        "documentIdentification" : {
          "standard" : "urn:no:difi:arkivmelding:xsd::arkivmelding_kvittering",
          "typeVersion" : "2.0",
          "instanceIdentifier" : "19efbd4c-413d-4e2c-bbc5-257ef4a65b38",
          "type" : "arkivmelding_kvittering",
          "creationDateAndTime" : "2019-03-25T12:38:18+01:00"
        },
        "businessScope" : {
          "scope" : [ {
            "type" : "ConversationId",
            "instanceIdentifier" : "19efbd4c-413d-4e2c-bbc5-257ef4a65b38",
            "identifier" : "urn:no:difi:profile:arkivmelding:response:ver1.0",
            "scopeInformation" : [ {
              "expectedResponseDateTime" : "2019-03-26T12:38:23+01:00"
            } ]
          } ]
        }
      },
      "arkivmelding_kvittering" : {
          "receiptType" : "OK",
          "relatedToMessageId" : "19efbd4c-413d-4e2c-bbc5-257ef4a65b38",
          "messages" : [ {
            "code" : "Recno",
            "text" : "315890"
          } ]
      }
    }
    """
    And the sent message contains no files
    And the message statuses for the conversation with id = "19efbd4c-413d-4e2c-bbc5-257ef4a65b38" are:
    """
    {
      "content" : [ {
        "id" : 39,
        "lastUpdate" : "2019-03-25T12:38:23+01:00",
        "status" : "OPPRETTET",
        "conversationId" : "19efbd4c-413d-4e2c-bbc5-257ef4a65b38",
        "messageId" : "19efbd4c-413d-4e2c-bbc5-257ef4a65b38",
        "convId" : 38
      }, {
        "id" : 40,
        "lastUpdate" : "2019-03-25T12:38:23+01:00",
        "status" : "SENDT",
        "conversationId" : "19efbd4c-413d-4e2c-bbc5-257ef4a65b38",
        "messageId" : "19efbd4c-413d-4e2c-bbc5-257ef4a65b38",
        "convId" : 38
      } ],
      "pageable" : {
        "sort" : {
          "unsorted" : false,
          "sorted" : true,
          "empty" : false
        },
        "offset" : 0,
        "pageNumber" : 0,
        "pageSize" : 10,
        "unpaged" : false,
        "paged" : true
      },
      "last" : true,
      "totalElements" : 2,
      "totalPages" : 1,
      "size" : 10,
      "number" : 0,
      "sort" : {
        "unsorted" : false,
        "sorted" : true,
        "empty" : false
      },
      "numberOfElements" : 2,
      "first" : true,
      "empty" : false
    }
    """
