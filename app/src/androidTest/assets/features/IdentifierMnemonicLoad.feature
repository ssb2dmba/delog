@mnemonic
Feature: Onboarding with mnemonic
  As a user, I want to be able to join the network with mnemonic

  Scenario: Load from mnemonic
    When I open application
    Then I open drawer
    Then I click 'identifiers'
    Then I click button 'new_identifier'
    Then "load identifier" text is presented
    Then I click 'from mnemonic'
    Then "Please select your 24 words mnemonic" text is presented
    Then I fill input with testTag "input_key" with value "abstract"
    Then I fill input with testTag "input_key" with value "panther"
    Then I fill input with testTag "input_key" with value "amount"
    Then I fill input with testTag "input_key" with value "steak"
    Then I fill input with testTag "input_key" with value "jacket"
    Then I fill input with testTag "input_key" with value "sight"
    Then I fill input with testTag "input_key" with value "media"
    Then I fill input with testTag "input_key" with value "model"
    Then I fill input with testTag "input_key" with value "march"
    Then I fill input with testTag "input_key" with value "panic"
    Then I fill input with testTag "input_key" with value "season"
    Then I fill input with testTag "input_key" with value "toilet"
    Then I fill input with testTag "input_key" with value "illness"
    Then I fill input with testTag "input_key" with value "dentist"
    Then I fill input with testTag "input_key" with value "twist"
    Then I fill input with testTag "input_key" with value "congress"
    Then I fill input with testTag "input_key" with value "fabric"
    Then I fill input with testTag "input_key" with value "rail"
    Then I fill input with testTag "input_key" with value "fog"
    Then I fill input with testTag "input_key" with value "hope"
    Then I fill input with testTag "input_key" with value "project"
    Then I fill input with testTag "input_key" with value "wild"
    Then I fill input with testTag "input_key" with value "stadium"
    Then I fill input with testTag "input_key" with value "bar"
    Then I click 'Restore from mnemonic'
    Then I submit webview passing succesfully the captcha
    Then I wait '1000'
    Then I fill input with testTag "alias" with value "test_mnemonic"
    Then I click 'save'
    Then I open drawer
    Then I click 'identifiers'
    Then I open drawer
    Then "test_mnemonic@delog.in" text is presented
