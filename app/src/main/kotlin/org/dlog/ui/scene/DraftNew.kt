package org.dlog.scene

import LocalActiveFeed
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.dlog.R
import org.dlog.db.model.Draft
import org.dlog.ui.navigation.Scenes
import org.dlog.viewmodel.DraftViewModel
import org.dlog.viewmodel.TopBarViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftNew(navController: NavHostController) {
    val feed = LocalActiveFeed.current ?: return
    val topBarViewModel= koinViewModel<TopBarViewModel>()
    val draftViewModel = koinViewModel<DraftViewModel>(parameters = { parametersOf(feed) })
    draftViewModel.setDirty()

    val strWhatUp = stringResource(id = R.string.whats_up)
    LaunchedEffect(Unit) {
        topBarViewModel.setTitle(strWhatUp.format(feed.alias))
    }

    LaunchedEffect(draftViewModel.inserted) {
        if(draftViewModel.inserted!=null) {
            navController.navigate("${Scenes.DraftEdit.route}/${draftViewModel.inserted}")
        }
    }

    var contentAsText by remember { mutableStateOf("") }

    Card(
        elevation = CardDefaults.cardElevation(),
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp, 8.dp)
                .fillMaxSize()
        ) {
            TextField(
                value = contentAsText,
                onValueChange = {
                    contentAsText = it
                },
                modifier = Modifier
                    .weight(1F)
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val draft = Draft(
                        0,
                        feed.publicKey,
                        System.currentTimeMillis(),
                        contentAsText
                    )
                    draftViewModel.insert(draft = draft)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(id = R.string.save_draft))
            }
        }
    }
}

fun navigate(navController: NavHostController, route: String) {

}

