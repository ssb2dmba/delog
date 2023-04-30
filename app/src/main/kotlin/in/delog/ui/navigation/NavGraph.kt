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
package `in`.delog.ui.navigation

import `in`.delog.ui.LocalActiveFeed
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import `in`.delog.ui.scene.*
import `in`.delog.ui.scene.identitifiers.IdentNew


@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Scenes.MainFeed.route
    )
    {
        val id = "id";
        composable(
            route = Scenes.MainFeed.route + "/{" + id + "}",
            arguments = listOf(navArgument(id) { type = NavType.StringType })
        ) { backStackEntry ->
            backStackEntry.arguments?.getString(id)
                ?.let { FeedMain(navController, it) }
        }
        composable(route = Scenes.MainFeed.route) {
            FeedMain(navController, LocalActiveFeed.current?.ident?.publicKey)
        }
        composable(route = Scenes.FeedList.route) {
            IdentList(navController)
        }
        composable(
            route = Scenes.FeedDetail.route + "/{" + id + "}",
            arguments = listOf(navArgument(id) { type = NavType.StringType })
        ) { backStackEntry ->
            backStackEntry.arguments?.getString(id)
                ?.let { IdentDetail(navController, it) }
        }
        composable(
            route = Scenes.AboutEdit.route + "/{" + id + "}",
            arguments = listOf(navArgument(id) { type = NavType.StringType })
        ) { backStackEntry ->
            backStackEntry.arguments?.getString(id)
                ?.let { AboutEdit(navController, it) }
        }
        composable(
            route = Scenes.FeedInit.route + "/{" + id + "}",
            arguments = listOf(navArgument(id) { type = NavType.StringType })
        ) { backStackEntry ->
            backStackEntry.arguments?.getString(id)
                ?.let { FeedInit(navController, it) }
        }
        composable(route = Scenes.NewFeed.route) {
            IdentNew(navController)
        }
        composable(route = Scenes.DraftList.route) {
            DraftList(navController)
        }
        composable(route = Scenes.ContactList.route) {
            ContactList(navController)
        }
        composable(route = Scenes.DraftNew.route) {
            DraftNew(navController)
        }
        composable(
            route = Scenes.DraftEdit.route + "/{" + id + "}",
            arguments = listOf(navArgument(id) { type = NavType.StringType })
        ) { backStackEntry ->
            backStackEntry.arguments?.getString(id)
                ?.let { DraftEdit(navController, it) }
        }

    }
}