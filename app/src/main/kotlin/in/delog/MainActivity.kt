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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import `in`.delog.service.ssb.ForkSsbService
import `in`.delog.ui.LocalActiveFeed
import `in`.delog.ui.component.AppBottomAppBar
import `in`.delog.ui.component.AppScaffold
import `in`.delog.ui.navigation.NavGraph
import `in`.delog.ui.theme.MyTheme
import `in`.delog.viewmodel.IdentListViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            MyTheme {
                MyApp()
            }
        }

    }

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApp() {

    val feedsViewModel = koinViewModel<IdentListViewModel>()
    val defaultFeed by feedsViewModel.default.observeAsState(null)
    CompositionLocalProvider(
        LocalActiveFeed provides defaultFeed
    ) {

        ForkSsbService()

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
                                .fillMaxHeight()
                        ) {

                            NavGraph(navController = navController)
                        }
                    },
                )
            }
        }
    }
}
