package `in`.delog.ui.component
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BottomBarMainButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier? = null
) {
    val m: Modifier = Modifier.height(36.dp)
    if (modifier != null) {
        m.then(modifier)
    }
    Button(
        modifier = m,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(),
        onClick = onClick
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}