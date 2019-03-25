Feature: Sending a Next Move DPE message

  Background:
#    Given a "GET" request to "http://localhost:9099/identifier/987464291?notification=obligated" will respond with status "200" and the following "application/json" in "/restmocks/identifier/987464291.json"
    Given a "GET" request to "http://localhost:9099/identifier/910075935?notification=obligated" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910075935.json"
#    And a "GET" request to "http://localhost:9099/identifier/910077473?notification=obligated" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910077473.json"

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
                                "expectedResponseDateTime": "2019-05-10T00:31:52Z"
                            }
                        ],
                        "identifier": "urn:no:difi:meldingsutveksling:2.0",
                        "instanceIdentifier": "45efbd4c-413d-4e2c-bbc5-257ef4a65a91",
                        "type": "ConversationId"
                    }
                ]
            },
            "documentIdentification": {
                "creationDateAndTime": "2019-04-11T15:29:58.753+02:00",
                "instanceIdentifier": "abc8849c-e281-4809-8555-7cd54952b916",
                "standard": "urn:no:difi:meldingsutveksling:2.0",
                "type": "DPE_INNSYN",
                "typeVersion": "2.0"
            },
            "headerVersion": "1.0",
            "receiver": [
                {
                    "identifier": {
                        "authority": "iso6523-actorid-upis",
                        "value": "9908:910075935"
                    }
                }
            ],
            "sender": [
                {
                    "identifier": {
                        "authority": "iso6523-actorid-upis",
                        "value": "9908:910077473"
                    }
                }
            ]
        },
        "dpe": {
          "securityLevel": "3",
          "primaryDocumentFilename": "test.txt"
        }
    }
    """
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
              "value" : "9908:910077473",
              "authority" : "iso6523-actorid-upis"
            },
            "contactInformation" : [ ]
          } ],
          "receiver" : [ {
            "identifier" : {
              "value" : "9908:910075935",
              "authority" : "iso6523-actorid-upis"
            },
            "contactInformation" : [ ]
          } ],
          "documentIdentification" : {
            "standard" : "urn:no:difi:meldingsutveksling:2.0",
            "typeVersion" : "2.0",
            "instanceIdentifier" : "abc8849c-e281-4809-8555-7cd54952b916",
            "type" : "DPE_INNSYN",
            "creationDateAndTime" : "2019-04-11T15:29:58.753+02:00"
          },
          "businessScope" : {
            "scope" : [ {
              "type" : "ConversationId",
              "instanceIdentifier" : "45efbd4c-413d-4e2c-bbc5-257ef4a65a91",
              "identifier" : "urn:no:difi:meldingsutveksling:2.0",
              "scopeInformation" : [ {
                "expectedResponseDateTime" : "2019-05-10T00:31:52Z"
              } ]
            } ]
          }
        },
        "dpe" : {
          "securityLevel" : "3",
          "primaryDocumentFilename" : "test.txt"
        }
      },
      "asic" : "content is hidden"
    }
    """
    And the sent ASIC contains the following files:
      | filename         |
      | manifest.xml     |
      | test.txt         |
    And the content of the ASIC file named "manifest.xml" is:
    """
    <?xml version="1.0" encoding="UTF-8"?>
    <manifest>
       <mottaker>
          <organisasjon authority="iso6523-actorid-upis">9908:910075935</organisasjon>
       </mottaker>
       <avsender>
          <organisasjon authority="iso6523-actorid-upis">9908:910077473</organisasjon>
       </avsender>
       <hoveddokument href="test.txt" mime="text/plain">
          <tittel lang="no">Hoveddokument</tittel>
       </hoveddokument>
    </manifest>
    """
    And the content of the ASIC file named "test.txt" is:
    """
    Testing 1 2 3
    """
