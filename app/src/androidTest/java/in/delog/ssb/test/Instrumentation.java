package in.delog.ssb.test;

//import cucumber.api.CucumberOptions;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import in.delog.MainApplication;
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
