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
        //format = {"junit:/data/data/com.mytest/JUnitReport.xml", "json:/data/data/com.mytest/JSONReport.json"},
        tags =  "not @ignore" ,
        features = "features"
)
public class Instrumentation  extends CucumberAndroidJUnitRunner{



    @Override
    public Application newApplication(ClassLoader cl, String className, Context context) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return super.newApplication(cl, MainApplication.class.getName(), context);
    }

    @Override
    public void onCreate(final Bundle bundle) {
        //bundle.putString("plugin", getPluginConfigurationString()); // we programmatically create the plugin configuration
        //it crashes on Android R without it
        //new File(getAbsoluteFilesPath()).mkdirs();
        super.onCreate(bundle);
    }



//
//    private static final String CUCUMBER_TAGS_KEY = "tags";
//    private static final String CUCUMBER_SCENARIO_KEY = "name";
//    private final CucumberInstrumentationCore instrumentationCore =
//            new CucumberInstrumentationCore(this);
//
//    @Override
//    public void onCreate(final Bundle bundle) {
//        String tags = "plop";
//        String scenario = "plop";
//        instrumentationCore.create(bundle);
//        super.onCreate(bundle);
//    }
//
//    @Override
//    public void onStart() {
//        waitForIdleSync();
//        instrumentationCore.start();
//    }
}
