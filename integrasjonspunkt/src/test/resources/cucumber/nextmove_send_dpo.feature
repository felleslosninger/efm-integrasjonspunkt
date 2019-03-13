Feature: Sending a Next Move DPO message

  Background:
    Given a "GET" request to "http://localhost:9099/identifier/987464291?notification=obligated" will respond with status "200" and the following "application/json" in "/restmocks/identifier/987464291.json"
    And a "GET" request to "http://localhost:9099/identifier/910075918?notification=obligated" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910075918.json"

  Scenario: As a user I want to send a DPO message
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
    And I upload a primary document named "primary.html" with mimetype "text/html" with the following body:
    """
    <h1>Primary document</h1>
    <p>This is the content of the primary document.</p>
    """
    And I upload a file named "before_the_law.txt" with mimetype "text/plain" and title "Before the law" with the following body:
    """
    Before the law sits a gatekeeper.
    """
    And I send the message
    Then a message with the same SBD is transported to C3
    And the transported ASIC contains the following files:
      | filename           |
      | manifest.xml       |
      | before_the_law.txt |
      | primary.html       |
    And the content of the ASIC file named "manifest.xml" is:
    """
    <?xml version="1.0" encoding="UTF-8"?>
    <manifest>
       <mottaker>
          <organisasjon authority="iso6523-actorid-upis">9908:910075918</organisasjon>
       </mottaker>
       <avsender>
          <organisasjon authority="iso6523-actorid-upis">9908:910077473</organisasjon>
       </avsender>
       <hoveddokument href="primary.html" mime="text/html">
          <tittel lang="no">Hoveddokument</tittel>
       </hoveddokument>
    </manifest>
    """
    And the content of the ASIC file named "primary.html" is:
    """
    <h1>Primary document</h1>
    <p>This is the content of the primary document.</p>
    """
    And the content of the ASIC file named "before_the_law.txt" is:
    """
    Before the law sits a gatekeeper.
    """
