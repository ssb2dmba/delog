Feature: Onboarding with new key
    As a user, I want to be able to join the network

  Scenario: Generate random identifier
    When I open application
    Then I open drawer
    Then I click 'identifiers'
    Then I click button 'new_identifier'
    Then "load new identifier" text is presented
    Then I click 'Generate random identifier'
    Then I submit webview passing succesfully the captcha
    Then I fill input with testTag "alias" with value "test random"
    Then I click 'save'
    Then I open drawer
    Then I click 'identifiers'
    Then "test random" text is presented



