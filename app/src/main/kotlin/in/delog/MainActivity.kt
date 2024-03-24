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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController

import `in`.delog.ui.LocalActiveFeed
import `in`.delog.ui.component.AppBottomAppBar
import `in`.delog.ui.component.AppScaffold
import `in`.delog.ui.navigation.NavGraph
import `in`.delog.ui.theme.MyTheme
import `in`.delog.viewmodel.IdentListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {

    private val app: MainApplication get() = application as MainApplication


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
                        }
                    },
                )
            }
        }
    }
}
