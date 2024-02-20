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
package `in`.delog.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import `in`.delog.db.model.IdentAndAboutWithBlob
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

val LocalActiveFeed = compositionLocalOf<IdentAndAboutWithBlob?> { null }

@Composable
fun <T> Flow<T>.observeAsState(
    initial: T,
    coroutineContext: CoroutineContext = EmptyCoroutineContext
): State<T> {
    val lifecycleOwner = LocalLifecycleOwner.current
    return remember(this, lifecycleOwner) {
        flowWithLifecycle(lifecycleOwner.lifecycle)
    }.collectAsState(initial = initial, coroutineContext)
}