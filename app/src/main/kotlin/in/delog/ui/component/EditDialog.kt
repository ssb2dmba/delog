package `in`.delog.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.delog.R

/**
 *  Maybe re-usable dialog able to edit a string field
 *  Somehow conform to Material3 guidelines
 *  as in https://m3.material.io/components/dialogs/specs
 *
 *  @param title
 *  @param field value
 *  @param closeDialog callback
 *  @param setValue callback
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDialog(
    title: Int,
    value: String,
    closeDialog: () -> Unit,
    setValue: (String) -> Unit
) {

    val txtField = remember { mutableStateOf(value) }

    AlertDialog(
        modifier = Modifier.padding(24.dp),
        onDismissRequest = {
            closeDialog()
        },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                color = MaterialTheme.colorScheme.onSurface,
                text = stringResource(id = title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .padding(bottom = 16.dp)
            )
        },
        text = {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Column(
                ) {

                    TextField(
                        modifier = Modifier
                            .fillMaxWidth(),
                        colors = TextFieldDefaults.textFieldColors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        placeholder = { Text(text = "Enter value") },
                        value = txtField.value,
                        onValueChange = {
                            txtField.value = it
                        })
                }
            }
        },
        dismissButton = {
            Text(
                text = stringResource(id = R.string.cancel),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp, top = 8.dp)
                    .clickable {
                        closeDialog()
                    }
            )
        },
        confirmButton = {
            Text(
                text = stringResource(R.string.save),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp, top = 8.dp)
                    .clickable {
                        setValue(txtField.value)
                    }
            )
        }
    )
}

@Preview
@Composable
fun previewEditDialog() {
    EditDialog(title = R.string.edit_default_invite_url,
        value = stringArrayResource(id = R.array.default_servers)[0],
        closeDialog = { },
        setValue = { }
    )
}