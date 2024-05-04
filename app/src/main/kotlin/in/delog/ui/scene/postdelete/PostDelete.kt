package `in`.delog.ui.scene.postdelete

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import `in`.delog.R
import `in`.delog.model.toMessageViewData
import `in`.delog.ui.component.LoadingAnimation
import `in`.delog.ui.component.MessageItem
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun PostDelete(
    navHostController: NavHostController,
    key: String
) {
    val viewModel = koinViewModel<MessageDeleteViewModel>(parameters = { parametersOf(key) })
    val messageEntity by viewModel.messageEntity.collectAsStateWithLifecycle(null)
    val done by viewModel.done.collectAsStateWithLifecycle(false)

    if (done) {
        navHostController.popBackStack()
        return
    }

    if (messageEntity == null) {
        LoadingAnimation()
        return
    }

    Surface {
        MessageItem(
            navController=navHostController,
            messageViewData = messageEntity!!.toMessageViewData(),
            onClickCallBack = {},
            showToolbar = false,
            truncate = true
        )
        AlertDialog(
            icon = {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete Icon",
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete Post") },
            text = { Text("Are you sure you want to delete this post?") },
            onDismissRequest = {
                navHostController.popBackStack()
            },
            dismissButton = {
                TextButton(
                    colors= ButtonDefaults.textButtonColors(),
                    onClick = {
                        navHostController.popBackStack()
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.dismiss),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePost()
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.delete),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        )
    }
}
