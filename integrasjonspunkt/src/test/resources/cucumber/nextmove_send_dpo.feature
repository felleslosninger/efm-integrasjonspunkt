Feature: Send a Next Move DPO message

  Background:
    Given a "GET" request to "http://localhost:9099/identifier/987464291?notification=obligated" will respond with status "200" and the following "application/json" in "/restmocks/identifier/987464291.json"
    And a "GET" request to "http://localhost:9099/identifier/910075918?notification=obligated" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910075918.json"

  Scenario: A user want to send a message
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
                        "instanceIdentifier": "37efbd4c-413d-4e2c-bbc5-257ef4a65a45",
                        "type": "ConversationId"
                    }
                ]
            },
            "documentIdentification": {
                "creationDateAndTime": "2019-04-11T15:29:58.753+02:00",
                "instanceIdentifier": "ff88849c-e281-4809-8555-7cd54952b916",
                "standard": "urn:no:difi:meldingsutveksling:2.0",
                "type": "DPO",
                "typeVersion": "2.0"
            },
            "headerVersion": "1.0",
            "receiver": [
                {
                    "identifier": {
                        "authority": "iso6523-actorid-upis",
                        "value": "9908:910075918"
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
        "dpo": {
          "securityLevel": "3",
          "dpoField": "foo"
        }
    }
    """
    And I upload a file named "before_the_law.txt" with mimetype "text/plain" and title "Before the law" with the following body:
    """
    Before the law sits a gatekeeper. To this gatekeeper comes a man from the country who asks to gain entry into the law.
    """