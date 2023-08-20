package `in`.delog.ui.component



import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import `in`.delog.R

@Composable
fun AppEmptyList() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            modifier = Modifier.padding(24.dp).align(Alignment.Center),
            text = stringResource(R.string.empty_list),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
