/**
 * Delog
 * Copyright (C) 2023 dmba.info
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package `in`.delog

import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import `in`.delog.ui.LocalActiveFeed
import `in`.delog.ui.component.AppBottomAppBar
import `in`.delog.ui.component.AppScaffold
import `in`.delog.ui.navigation.NavGraph
import `in`.delog.ui.navigation.Scenes
import `in`.delog.ui.theme.MyTheme
import `in`.delog.viewmodel.IdentListViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.io.File

class MainActivity : ComponentActivity() {

    private var pressedTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            MyTheme {
                MyApp()
            }
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            externalCacheDir?.let { deleteTempFiles(it) }
        }
    }

    private fun deleteTempFiles(file: File): Boolean {
        if (file.isDirectory) {
            val files: Array<File> = file.listFiles()
            if (files != null) {
                for (f in files) {
                    if (f.isDirectory) {
                        deleteTempFiles(f)
                    } else {
                        f.delete()
                    }
                }
            }
        }
        return file.delete()
    }

    // on below line we are calling on back press method.
    /**
    override fun onBackPressed() {
        // on below line we are checking if the press time is greater than 2 sec
        if (pressedTime + 2000 > System.currentTimeMillis()) {
            // if time is greater than 2 sec we are closing the application.
            super.onBackPressedDispatcher.onBackPressed()
            finish()
        } else {
            // in else condition displaying a toast message.
            Toast.makeText(getBaseContext(), "Press back again to exit", Toast.LENGTH_SHORT).show();
        }
        // on below line initializing our press time variable
        pressedTime = System.currentTimeMillis();
    }
    */

}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyApp() {

    val feedsViewModel = koinViewModel<IdentListViewModel>()
    val defaultFeed by feedsViewModel.default.observeAsState(null)
    CompositionLocalProvider(
        LocalActiveFeed provides defaultFeed
    ) {

        val context = MainApplication.applicationContext()
        var darkTheme = true
        when (context.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> {
                darkTheme = true
            }

            Configuration.UI_MODE_NIGHT_NO -> {
                darkTheme = false
            }

            Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                darkTheme = true
            }
        }

        MyTheme(darkTheme = darkTheme) {
            Surface(
                modifier = Modifier.fillMaxSize()
            ) {
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val coroutineScope = rememberCoroutineScope()
                val navController = rememberNavController()
                AppScaffold(
                    drawerState = drawerState,
                    navController = navController,
                    bottomBar = {
                        AppBottomAppBar {
                            coroutineScope.launch {
                                drawerState.open()
                            }
                        }
                    },
                    floatingActionButtonPosition = FabPosition.Center,
                    floatingActionButton = { },

                    content = {
                        Surface(
                            modifier = Modifier
                                .padding(it)
                                .consumeWindowInsets(it)
                                .systemBarsPadding()
                                .fillMaxHeight()
                        ) {
                            NavGraph(navController = navController)
                            navController.navigate(Scenes.MainFeed.route)
                        }
                    },
                )
            }
        }
    }



}
