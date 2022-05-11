Feature: Capabilities

  Background:
    Given a "GET" request to "http://localhost:9099/identifier/910077473" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910077473.json"
    Given a "GET" request to "http://localhost:9099/identifier/910075935" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910075935.json"
    Given a "GET" request to "http://localhost:9099/identifier/09118532322" will respond with status "200" and the following "application/json" in "/restmocks/identifier/09118532322.json"

  Scenario: As a user I want to get a list of all capabilities for 910077473
    Given I request all capabilities for "910077473"
    Then the returned capabilities are:
      """
      {
        "capabilities" : [ {
          "process" : "urn:no:difi:profile:arkivmelding:administrasjon:ver1.0",
          "serviceIdentifier" : "DPO",
          "documentTypes" : [ {
            "type" : "arkivmelding",
            "standard" : "urn:no:difi:arkivmelding:xsd::arkivmelding"
          } ]
        }, {
          "process" : "urn:no:difi:profile:arkivmelding:response:ver1.0",
          "serviceIdentifier" : "DPO",
          "documentTypes" : [ {
            "type" : "arkivmelding_kvittering",
            "standard" : "urn:no:difi:arkivmelding:xsd::arkivmelding_kvittering"
          }, {
            "type" : "status",
            "standard" : "urn:no:difi:arkivmelding:xsd::status"
          }, {
            "type" : "feil",
            "standard" : "urn:no:difi:arkivmelding:xsd::feil"
          } ]
        }, {
          "process" : "urn:no:difi:profile:einnsyn:response:ver1.0",
          "serviceIdentifier" : "DPE",
          "documentTypes" : [ {
            "type" : "einnsyn_kvittering",
            "standard" : "urn:no:difi:einnsyn:xsd::einnsyn_kvittering"
          }, {
            "type" : "status",
            "standard" : "urn:no:difi:eformidling:xsd::status"
          }, {
            "type" : "feil",
            "standard" : "urn:no:difi:eformidling:xsd::feil"
          } ]
        } ]
      }
    """

  Scenario: As a user I want to get a list of all capabilities for 910075935
    Given I request all capabilities for "910075935"
    Then the returned capabilities are:
      """
      {
      "capabilities" :
      [ {
        "process" : "urn:no:difi:profile:einnsyn-innsynskrav:ver1.0",
        "serviceIdentifier" : "DPE",
        "documentTypes" : [ {
          "type" : "innsynskrav",
          "standard" : "urn:no:difi:einnsyn:xsd:innsyn::innsynskrav"
        } ]
      } ]
      }
      """

  Scenario: As a user I want to get a list of all capabilities for 09118532322
    Given I request all capabilities for "09118532322"
    Then the returned capabilities are:
      """
      {
        "capabilities" : [ {
          "process" : "urn:no:difi:profile:digitalpost:info:ver1.0",
          "serviceIdentifier" : "DPI",
          "returnAddress" : {
            "name" : "",
            "street" : "",
            "postalCode" : "",
            "postalArea" : "",
            "country" : ""
          },
          "postAddress" : {
            "name" : "",
            "street" : "",
            "postalCode" : "",
            "postalArea" : "",
            "country" : ""
          },
          "documentTypes" : [ {
            "type" : "digital",
            "standard" : "urn:no:difi:digitalpost:xsd:digital::digital"
          }, {
            "type" : "print",
            "standard" : "urn:no:difi:digitalpost:xsd:fysisk::print"
          } ],
          "digitalPostAddress": {
                "supplier": "987464291",
                "address": "dummy"
          }
        }, {
          "process" : "urn:no:difi:profile:digitalpost:vedtak:ver1.0",
          "serviceIdentifier" : "DPI",
          "returnAddress" : {
            "name" : "",
            "street" : "",
            "postalCode" : "",
            "postalArea" : "",
            "country" : ""
          },
          "postAddress" : {
            "name" : "",
            "street" : "",
            "postalCode" : "",
            "postalArea" : "",
            "country" : ""
          },
          "documentTypes" : [ {
            "type" : "digital",
            "standard" : "urn:no:difi:digitalpost:xsd:digital::digital"
          }, {
            "type" : "print",
            "standard" : "urn:no:difi:digitalpost:xsd:fysisk::print"
          } ],
          "digitalPostAddress": {
                "supplier": "987464291",
                "address": "dummy"
          }
        } ]
      }
      """
