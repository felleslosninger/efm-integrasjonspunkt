Feature: Some terse yet descriptive text of what is desired
 2:   Textual description of the business value of this feature
 3:   Business rules that govern the scope of the feature
 4:   Any additional information that will make the feature easier to understand
 5: 
   Scenario: Some determinable business situation
     Given some precondition
       And some other precondition
      When some action by the actor
       And some other action
       And yet another action
      Then some testable outcome is achieved
       And something else we can check happens too

   Scenario: A different situation
#trenger ikke begynne med Given

#Feature: Serve coffee
#    Coffee should not be served until paid for
#    Coffee should not be served until the button has been pressed
#    If there is no coffee left then money should be refunded
#
#  Scenario: Buy last coffee
#    Given there are 1 coffees left in the machine
#    And I have deposited 1$
#    When I press the coffee button
#    Then I should be served a coffee

# language: no
#Egenskap: Summering
#  For å unngå at firmaet går konkurs
#  Må regnskapsførerere bruke en regnemaskin for å legge sammen tall
#
#  Scenario: to tall
#    Gitt at jeg har tastet inn 5
#    Og at jeg har tastet inn 7
#    Når jeg summerer
#    Så skal resultatet være 12
#
#  Scenario: tre tall
#    Gitt at jeg har tastet inn 5
#    Og at jeg har tastet inn 7
#    Og at jeg har tastet inn 1
#    Når jeg summerer
#    Så skal resultatet være 13