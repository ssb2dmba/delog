package org.dlog.scene

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import org.dlog.R
import org.dlog.db.model.About
import org.dlog.ssb.*
import org.dlog.ui.navigation.Scenes
import org.dlog.ui.theme.keySmall
import org.dlog.viewmodel.IdentViewModel
import org.dlog.viewmodel.TopBarViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.util.*


@Composable
fun AboutEdit(
    navHostController: NavHostController,
    id: String
) {
    val vm = koinViewModel<IdentViewModel>(parameters = { parametersOf(id) })

    val topBarViewModel = koinViewModel<TopBarViewModel>()
    val title = stringResource(R.string.about)

    val showExportDialogState: Boolean by vm.showExportDialog.collectAsState()

    LaunchedEffect(id) {
        vm.setCurrentIdent(id)
    }
    if (vm.about == null) {
        return
    }


    topBarViewModel.setActions { IdentDetailTopBarMenu(navHostController, vm) }
    topBarViewModel.setTitle(title + " " + vm.ident!!.alias)
    AboutEditForm(about = vm.about!!, onSaveAbout = fun(it: About) {
        vm.onOpenExportDialogClicked();
    })


    if (showExportDialogState) {
        AboutEditPublishDialog(navHostController, vm)
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutEditForm(about: About, onSaveAbout: (About) -> Unit) {

    var dirty by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(about.name) }
    var description by remember { mutableStateOf(about.description) }
    var image by remember { mutableStateOf(about.image) }

    Card(
        elevation = CardDefaults.cardElevation(),
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .fillMaxHeight()
    ) {

        Column(
            modifier = Modifier
                .padding(12.dp, 8.dp)
                .fillMaxWidth()
        ) {
            // public key
            Row {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = about.about,
                    style = keySmall,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row {
                AsyncImage(
                    model = "https://robohash.org/${about.about}.png",
                    placeholder = rememberAsyncImagePainter("https://robohash.org/${about.about}.png"),
                    contentDescription = "Profile Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(top = 12.dp, end = 8.dp)
                        .size(size = 48.dp)
                        .clip(shape = CircleShape)
                        .background(MaterialTheme.colorScheme.outline),
                )

                // name
                OutlinedTextField(
                    value = if (name != null) name!! else "",
                    onValueChange = { value ->
                        dirty = true
                        if (value != "") name = value
                    },
                    label = {
                        Text(
                            text = stringResource(id = R.string.alias),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    modifier = Modifier.weight(0.8f)
                )

            }

        }
        Spacer(modifier = Modifier.height(16.dp))


        // description
        OutlinedTextField(
            value = if (description != null) description!! else "",
            onValueChange = { value ->
                dirty = true
                if (value != "") description = value
            },
            label = {
                Text(
                    text = stringResource(id = R.string.description),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp, 8.dp)
        )


        // save
        Button(
            enabled = dirty,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            onClick = {

                      onSaveAbout(about)
            },
            modifier = Modifier
                .padding(12.dp, 8.dp)
                .fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.save))
        }
    }
}

@Composable
fun AboutEditPublishDialog(
    navHostController: NavHostController,
    viewModel: IdentViewModel
) {
    AlertDialog(onDismissRequest = { viewModel.onExportDialogDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                color = MaterialTheme.colorScheme.onSurface,
                text = stringResource(id = R.string.publish_about),
                style = MaterialTheme.typography.titleSmall
            )
        },
        text = {
            Text(
                text = stringResource(id = R.string.publish),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        dismissButton = {
            Text(
                text = stringResource(id = R.string.dismiss),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(15.dp)
                    .clickable { viewModel.onExportDialogDismiss() }
            )
        },
        confirmButton = {
            Text(
                text = stringResource(R.string.publish),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(15.dp)
                    .clickable {
                        viewModel.onPublishDialogClicked()
                        navHostController.navigate("${Scenes.FeedList.route}")
                    }
            )
        }
    )
}
