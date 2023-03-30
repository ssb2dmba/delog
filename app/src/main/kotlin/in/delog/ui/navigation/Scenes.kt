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

sealed class Scenes(val route: String) {
    object FeedList : Scenes("feed_list")
    object NewFeed : Scenes("new_feed")
    object FeedDetail : Scenes("feed_detail")
    object FeedInit : Scenes("feed_init")
    object AboutEdit : Scenes("about_edit")
    object MainFeed : Scenes("main_feed")
    object DraftList : Scenes("draft_list")
    object DraftNew : Scenes("draft_new")
    object DraftEdit : Scenes("draft_edit")
    object ContactList : Scenes("contact_list")
}