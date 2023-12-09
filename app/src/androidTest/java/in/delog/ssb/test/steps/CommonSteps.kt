package `in`.delog.ssb.test.steps

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.webClick
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.platform.app.InstrumentationRegistry
import `in`.delog.MainActivity
import io.cucumber.java.en.Then
import io.cucumber.java.en.When

class CommonSteps(
    val composeRuleHolder: ComposeRuleHolder,
    val scenarioHolder: ActivityScenarioHolder
) : SemanticsNodeInteractionsProvider by composeRuleHolder.composeRule {

    val sleep = 100L // used to add some wait to visualize says 1000L

    @When("^I open application$")
    fun iOpenComposeActivity() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        scenarioHolder.launch(Intent(instrumentation.targetContext, MainActivity::class.java))
    }

    @Then("^\"([^\"]*)\" text is presented$")
    fun textIsPresented(arg0: String) {
        onAllNodesWithText(arg0)[0].assertIsDisplayed()
    }


    @Then("I click {string}")
    fun I_click(s: String) {
        Thread.sleep(sleep)
        onNodeWithText(s).performClick();
        Thread.sleep(sleep)
    }

    @Then("I click element with testTag {string}")
    fun I_clickTestTag(s: String) {
        Thread.sleep(sleep)
        onNodeWithTag(s).performClick();
        Thread.sleep(sleep)
    }

    @Then("I click button {string}")
    fun I_click2(s: String) {
        Thread.sleep(sleep)
        onNodeWithTag(s).performClick();
        Thread.sleep(sleep)
    }

    @Then("I open drawer")
    fun I_open_drawer() {
        Thread.sleep(sleep)
        onNode(hasTestTag("openDrawer")).performClick();
        Thread.sleep(sleep)
    }

    @Then("I submit webview passing succesfully the captcha")
    fun I_submit_webview() {
        Thread.sleep(sleep)
        onWebView().withElement(findElement(Locator.ID, "captcha"))
            .perform(DriverAtoms.webKeys("1234"))
        onWebView().withElement(findElement(Locator.NAME, "action")).perform(webClick())
        Thread.sleep(sleep)
    }

    @Then("I fill input with testTag {string} with value {string}")
    fun I_fill_input(testTag: String, value: String) {
        Thread.sleep(sleep)
        onNode(hasTestTag(testTag)).performTextInput(value)
        Thread.sleep(sleep)
    }


    @Then("I wait {string}")
    fun I_wait(value: String) {
        Thread.sleep(value.toLong())
    }

}