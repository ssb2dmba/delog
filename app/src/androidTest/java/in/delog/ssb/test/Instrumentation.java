package in.delog.ssb.test;


import io.cucumber.android.runner.CucumberAndroidJUnitRunner;
import io.cucumber.junit.CucumberOptions;


@SuppressWarnings("unused")
@CucumberOptions(
        glue = "in.delog.ssb.test",
        tags =  "not @ignore" ,
        features = "features"
)
public class Instrumentation  extends CucumberAndroidJUnitRunner{

}
