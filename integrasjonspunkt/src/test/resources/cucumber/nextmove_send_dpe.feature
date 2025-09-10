Feature: Sending a Next Move DPE message

  Background:
    Given a "GET" request to "http://localhost:9099/identifier/910075935" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910075935.json"
    And a "GET" request to "http://localhost:9099/identifier/910075935/process/urn:no:difi:profile:einnsyn-innsynskrav:ver1.0?conversationId=21efbd4c-413d-4e2c-bbc5-257ef4a65a91" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910075935-innsynskrav.json"
    And a "GET" request to "http://localhost:9099/identifier/910077473" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910077473.json"
    And a "GET" request to "http://localhost:9099/virksert/910077473" will respond with status "200" and the following "text/plain" in "/restmocks/virksert/910077473"

  Scenario: As a user I want to send a DPE message
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
                        "identifier": "urn:no:difi:profile:einnsyn-innsynskrav:ver1.0",
                        "instanceIdentifier": "21efbd4c-413d-4e2c-bbc5-257ef4a65a91",
                        "type": "ConversationId"
                    }
                ]
            },
            "documentIdentification": {
                "creationDateAndTime": "2019-03-25T11:35:00+01:00",
                "instanceIdentifier": "20c8849c-e281-4809-8555-7cd54952b916",
                "standard": "urn:no:difi:einnsyn:xsd:innsyn::innsynskrav",
                "type": "innsynskrav",
                "typeVersion": "2.0"
            },
            "headerVersion": "1.0",
            "receiver": [
                {
                    "identifier": {
                        "authority": "iso6523-actorid-upis",
                        "value": "0192:910075935"
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
        "innsynskrav" : {
          "orgnr" : "98765432",
          "epost" : "doofenshmirtz@evil.inc"
        }
    }
    """
    And the response status is "OK"
    And I upload a file named "test.txt" with mimetype "text/plain" and title "Test" with the following body:
    """
    Testing 1 2 3
    """
    And I send the message
    Then a POST to the ServiceBus is initiated with:
    """
    {
      "sbd" : {
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
              "value" : "0192:910075935",
              "authority" : "iso6523-actorid-upis"
            }
          } ],
          "documentIdentification" : {
            "standard" : "urn:no:difi:einnsyn:xsd:innsyn::innsynskrav",
            "typeVersion" : "2.0",
            "instanceIdentifier" : "20c8849c-e281-4809-8555-7cd54952b916",
            "type" : "innsynskrav",
            "creationDateAndTime" : "2019-03-25T11:35:00+01:00"
          },
          "businessScope" : {
            "scope" : [ {
              "type" : "ConversationId",
              "instanceIdentifier" : "21efbd4c-413d-4e2c-bbc5-257ef4a65a91",
              "identifier" : "urn:no:difi:profile:einnsyn-innsynskrav:ver1.0",
              "scopeInformation" : [ {
                "expectedResponseDateTime" : "2019-05-10T00:31:52+01:00"
              } ]
            } ]
          }
        },
        "innsynskrav" : {
          "orgnr" : "98765432",
          "epost" : "doofenshmirtz@evil.inc"
        }
      },
      "asic" : "content is hidden"
    }
    """
    And the sent message contains the following files:
      | filename     | content type |
      | manifest.xml | application/xml     |
      | test.txt     | text/plain   |
    And the content of the file named "manifest.xml" is:
    """
    <?xml version="1.0" encoding="UTF-8"?>
    <manifest>
       <mottaker>
          <organisasjon authority="iso6523-actorid-upis">0192:910075935</organisasjon>
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
