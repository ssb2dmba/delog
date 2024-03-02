package `in`.delog.ui.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Pageview
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import `in`.delog.R


@Composable
fun CancelIcon() {
    Icon(
        imageVector = Icons.Default.Cancel,
        null,
        modifier = Modifier.fillMaxSize(),
        tint = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

@Composable
fun PageViewIcon() {
    Icon(
        imageVector = Icons.Default.Pageview,
        null,
        modifier = Modifier.fillMaxSize(),
        tint = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

@Composable
fun ArrowBackIcon() {
    Icon(
        imageVector = Icons.Default.ArrowBack,
        contentDescription = stringResource(R.string.back),
        tint = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

@Composable
fun ArrowAfterIcon() {
    Icon(
        imageVector = Icons.Default.ArrowForward,
        contentDescription = stringResource(R.string.forward),
        tint = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}