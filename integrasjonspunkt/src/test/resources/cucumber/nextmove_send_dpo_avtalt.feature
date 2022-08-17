Feature: Sending a Next Move DPO message of forretningstype avtalt

  Background:
    Given a "GET" request to "http://localhost:9099/identifier/0192:910075918" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910075918.json"
    And a "GET" request to "http://localhost:9099/identifier/0192:910075918/process/urn:no:difi:profile:avtalt:avtalt:ver1.0?securityLevel=3&conversationId=37efbd4c-413d-4e2c-bbc5-257ef4a65a56" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910075918-avtalt.json"
    And a "GET" request to "http://localhost:9099/identifier/0192:910075918/process/urn:no:difi:profile:avtalt:avtalt:ver1.0?conversationId=37efbd4c-413d-4e2c-bbc5-257ef4a65a56" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910075918-avtalt.json"
    And a "GET" request to "http://localhost:9099/virksert/0192:910077473" will respond with status "200" and the following "text/plain" in "/restmocks/virksert/910077473"
    And the Noark System is disabled

  Scenario: As a user I want to send a DPO message with forretningstype avtalt
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
                        "identifier": "urn:no:difi:profile:avtalt:avtalt:ver1.0",
                        "instanceIdentifier": "37efbd4c-413d-4e2c-bbc5-257ef4a65a56",
                        "type": "ConversationId"
                    }
                ]
            },
            "documentIdentification": {
                "creationDateAndTime": "2019-03-25T11:35:00+01:00",
                "instanceIdentifier": "ff88849c-e281-4809-8555-7cd54952c916",
                "standard": "urn:no:difi:avtalt:xsd::avtalt",
                "type": "avtalt",
                "typeVersion": "2.0"
            },
            "headerVersion": "1.0",
            "receiver": [
                {
                    "identifier": {
                        "authority": "iso6523-actorid-upis",
                        "value": "0192:910075918"
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
        "avtalt": {
         "identifier" : "foo",
         "content": {
         "innhold" : "momomomomo"}
          }
    }
    """
    And the response status is "OK"
    And I upload a file named "test.txt" with mimetype "text/plain" and title "Test" with the following body:
    """
    Testing 1 2 3
    """
    And I send the message
    Then an upload to Altinn is initiated with:
    """
    <?xml version='1.0' encoding='UTF-8'?>
    <S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
      <S:Body>
        <ns2:InitiateBrokerServiceBasic xmlns="http://schemas.altinn.no/services/ServiceEngine/Broker/2015/06" xmlns:ns2="http://www.altinn.no/services/ServiceEngine/Broker/2015/06" xmlns:ns3="http://www.altinn.no/services/common/fault/2009/10" xmlns:ns4="http://www.altinn.no/services/2009/10" xmlns:ns5="http://schemas.microsoft.com/2003/10/Serialization/" xmlns:ns6="http://schemas.altinn.no/services/serviceEntity/2015/06">
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
              <SendersReference>19efbd4c-413d-4e2c-bbc5-257ef4a65b38</SendersReference>
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
      | asic.zip       |
    And the content of the Altinn ZIP file named "manifest.xml" is:
    """
    <?xml version="1.0" encoding="UTF-8"?>
    <BrokerServiceManifest xmlns="http://schema.altinn.no/services/ServiceEngine/Broker/2015/06">
       <ExternalServiceCode>v3888</ExternalServiceCode>
       <ExternalServiceEditionCode>70515</ExternalServiceEditionCode>
       <SendersReference>19efbd4c-413d-4e2c-bbc5-257ef4a65b38</SendersReference>
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
          "standard" : "urn:no:difi:avtalt:xsd::avtalt",
          "typeVersion" : "2.0",
          "instanceIdentifier" : "ff88849c-e281-4809-8555-7cd54952c916",
          "type" : "avtalt",
          "creationDateAndTime" : "2019-03-25T11:35:00+01:00"
        },
        "businessScope" : {
          "scope" : [ {
            "type" : "ConversationId",
            "instanceIdentifier" : "37efbd4c-413d-4e2c-bbc5-257ef4a65a56",
            "identifier" : "urn:no:difi:profile:avtalt:avtalt:ver1.0",
            "scopeInformation" : [ {
              "expectedResponseDateTime" : "2019-05-10T00:31:52+01:00"
            } ]
          } ]
        }
      },
         "avtalt": {
         "identifier" : "foo",
         "content": {
         "innhold" : "momomomomo"}
      }
    }
    """
    And the sent message contains the following files:
      | filename         | content type |
      | manifest.xml     | application/xml     |
      | test.txt         | text/plain   |
    And the content of the file named "manifest.xml" is:
    """
  <?xml version="1.0" encoding="UTF-8"?>
<manifest>
   <mottaker>
      <organisasjon authority="iso6523-actorid-upis">0192:910075918</organisasjon>
   </mottaker>
   <avsender>
      <organisasjon authority="iso6523-actorid-upis">0192:910077473</organisasjon>
   </avsender>
   <hoveddokument href="test.txt" mime="text/plain">
      <tittel lang="no">Hoveddokument</tittel>
   </hoveddokument>
</manifest>
    """

    And the content of the file named "test.txt" is:
    """
    Testing 1 2 3
    """
    And the message statuses for the conversation with id = "ff88849c-e281-4809-8555-7cd54952c916" are:
    """
    {
      "content" : [ {
        "id" : 167,
        "lastUpdate" : "2019-03-25T12:38:23+01:00",
        "status" : "OPPRETTET",
        "conversationId" : "37efbd4c-413d-4e2c-bbc5-257ef4a65a56",
        "messageId" : "ff88849c-e281-4809-8555-7cd54952c916",
        "convId" : 166
      }, {
        "id" : 170,
        "lastUpdate" : "2019-03-25T12:38:23+01:00",
        "status" : "SENDT",
        "conversationId" : "37efbd4c-413d-4e2c-bbc5-257ef4a65a56",
        "messageId" : "ff88849c-e281-4809-8555-7cd54952c916",
        "convId" : 166
      } ],
      "pageable" : {
        "sort" : {
          "sorted" : true,
          "unsorted" : false,
          "empty" : false
        },
        "offset" : 0,
        "pageSize" : 10,
        "pageNumber" : 0,
        "paged" : true,
        "unpaged" : false
      },
      "last" : true,
      "totalElements" : 2,
      "totalPages" : 1,
      "size" : 10,
      "number" : 0,
      "first" : true,
      "sort" : {
        "sorted" : true,
        "unsorted" : false,
        "empty" : false
      },
      "numberOfElements" : 2,
      "empty" : false
    }
    """
