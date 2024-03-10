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

import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import `in`.delog.ui.LocalActiveFeed
import `in`.delog.ui.scene.AboutEdit
import `in`.delog.ui.scene.ContactList
import `in`.delog.ui.scene.DraftEdit
import `in`.delog.ui.scene.DraftList
import `in`.delog.ui.scene.IdentDetail
import `in`.delog.ui.scene.IdentList
import `in`.delog.ui.scene.MessagesList
import `in`.delog.ui.scene.PreferencesEdit
import `in`.delog.ui.scene.identitifiers.IdentNew

const val LINK = "link";
const val TYPE = "draftType";
const val OID = "id";
@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Scenes.MainFeed.route
    )
    {
        composable(
            route = "share_target_route",
            deepLinks = listOf(
                navDeepLink {
                    action = Intent.ACTION_SEND
                    mimeType = "image/*"
                },
                navDeepLink {
                    action = Intent.ACTION_SEND_MULTIPLE
                    mimeType = "image/*"
                },


                )
        ) {
            Log.i("NavGraph", "share_target_route")
            DraftEdit(navController = navController, draftMode ="post",  draftId = 0L, link= "")
        }
        composable(
            route = Scenes.MainFeed.route + "/{" + LINK + "}",
            arguments = listOf(navArgument(LINK) { type = NavType.StringType })
        ) { backStackEntry ->
            backStackEntry.arguments?.getString(LINK)
                ?.let { MessagesList(navController, it) }
        }




        composable(route = Scenes.MainFeed.route) {
            LocalActiveFeed.current?.ident?.publicKey?.let { it1 ->
                MessagesList(
                    navController,
                    it1
                )
            }
        }
        composable(route = Scenes.FeedList.route) {
            IdentList(navController)
        }
        composable(
            route = Scenes.FeedDetail.route + "/{" + LINK + "}",
            arguments = listOf(navArgument(LINK) { type = NavType.StringType })
        ) { backStackEntry ->
            backStackEntry.arguments?.getString(LINK)
                ?.let { IdentDetail(navController, it) }
        }
        composable(
            route = Scenes.AboutEdit.route + "/{" + LINK + "}",
            arguments = listOf(navArgument(LINK) { type = NavType.StringType })
        ) { backStackEntry ->
            backStackEntry.arguments?.getString(LINK)
                ?.let { AboutEdit(navController, it) }
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


        composable(route = Scenes.Preferences.route) {
            PreferencesEdit(navController)
        }

        composable(
            route = Scenes.DraftNew.route + "/post",
        ) { backStackEntry ->
            DraftEdit(navController = navController, draftMode ="post",  draftId = 0L, link= "")
        }

        composable(
            route = Scenes.DraftNew.route + "/{" + TYPE + "}" + "/{" + LINK + "}",
            arguments = listOf(
                navArgument(LINK) { type = NavType.StringType },
                navArgument(TYPE) { type = NavType.StringType })
        ) { backStackEntry ->
            var linkKey = backStackEntry.arguments?.getString(LINK)
            val draftType = backStackEntry.arguments?.getString(TYPE)
            if (linkKey == null) linkKey = ""
            DraftEdit(navController = navController, draftMode =draftType!!,  draftId = 0L, link= linkKey!!)
        }




        composable(
            route = Scenes.DraftEdit.route + "/{" + OID + "}",
            arguments = listOf(navArgument(OID) { type = NavType.LongType})
        ) { backStackEntry ->
            var oid = backStackEntry.arguments?.getLong(OID)
            if (oid == null) oid = 0
            DraftEdit(navController, draftMode="", draftId= oid,link="")
        }

    }
}


