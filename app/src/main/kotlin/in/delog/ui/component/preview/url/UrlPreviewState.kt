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
package `in`.delog.ui.component.preview.url

import androidx.compose.runtime.Immutable


@Immutable
sealed class UrlPreviewState {

    @Immutable
    object Loading : UrlPreviewState()

    @Immutable
    class Loaded(val previewInfo: UrlInfoItem) : UrlPreviewState()

    @Immutable
    object Empty : UrlPreviewState()

    @Immutable
    class Error(val errorMessage: String) : UrlPreviewState()
}