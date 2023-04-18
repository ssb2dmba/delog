package `in`.delog.ssb.test.steps

import android.content.Intent
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import io.cucumber.java.en.Then
import org.picocontainer.annotations.Inject
import io.cucumber.java.en.When
import cucumber.api.PendingException
import `in`.delog.MainActivity
import `in`.delog.R
import `in`.delog.ssb.test.ComposeRuleHolder

class KotlinSteps(val composeRuleHolder: ComposeRuleHolder, val scenarioHolder: ActivityScenarioHolder):SemanticsNodeInteractionsProvider by composeRuleHolder.composeRule {


    @When("^I open application$")
    fun iOpenComposeActivity() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        scenarioHolder.launch(Intent(instrumentation.targetContext, MainActivity::class.java))
    }

    @Then("^\"([^\"]*)\" text is presented$")
    fun textIsPresented(arg0: String) {
        onNodeWithText(arg0).assertIsDisplayed()
    }


    @Then("I should see {string} on the display")
    fun I_should_see_s_on_the_display(s: String?) {
        Espresso.onView(withText(s)).check(ViewAssertions.matches(ViewMatchers.withText(s)))
    }




}