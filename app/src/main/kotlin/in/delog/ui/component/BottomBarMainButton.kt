package `in`.delog.ui.component

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
    modifier: Modifier?=null
) {
    val m: Modifier = Modifier.height(36.dp)
    if (modifier!=null) {
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