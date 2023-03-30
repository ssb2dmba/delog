package org.dlog.scene

import LocalActiveFeed
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PermIdentity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import org.dlog.db.model.Contact
import org.dlog.db.model.Ident
import org.dlog.ui.navigation.Scenes
import org.dlog.ui.theme.keySmall
import org.dlog.viewmodel.ContactListViewModel
import org.dlog.viewmodel.IdentViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


@Composable
fun ContactList(navController: NavController) {
    val feed = LocalActiveFeed.current ?: return
    val contactListViewModel = koinViewModel<ContactListViewModel>(parameters = { parametersOf(feed.publicKey) })
    val fpgDrafts: Flow<PagingData<Contact>> = contactListViewModel.contactsPaged
    val lazyContactItems: LazyPagingItems<Contact> = fpgDrafts.collectAsLazyPagingItems()
    LazyVerticalGrid(columns = GridCells.Fixed(1)) {
        items(
            count = lazyContactItems.itemCount,
        ) { index ->
            lazyContactItems[index]?.let {
                ContactListItem(contact = it, navController)
            }
        }
    }
}


@Composable
fun ContactListItem(contact: Contact, navController: NavController) {
    Card(
        shape = RoundedCornerShape(0.dp),
        elevation = CardDefaults.cardElevation(),
        modifier = Modifier
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .clickable {
                // open menu: unfollow||block
            }
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp, 4.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = contact.follow,
                //color=MaterialTheme.colorScheme.primary,
                style = keySmall,
            )
        }

        Row(
            modifier = Modifier
                .padding(start = 12.dp, bottom = 8.dp)
                .fillMaxWidth()
        ) {

            Image(
                modifier = Modifier
                    .padding(top = 12.dp, end = 8.dp)
                    .size(size = 48.dp)
                    .clip(shape = CircleShape)
                    .background(MaterialTheme.colorScheme.onSecondaryContainer),
                painter = rememberVectorPainter(Icons.Rounded.PermIdentity),
                contentDescription = "Profile Image"
            )
            Row(Modifier.fillMaxWidth()) {
                Text(
                    modifier = Modifier.padding(top = 24.dp).weight(0.85f),
                    text = contact.follow,
                    //color=MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }
    }
}

//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun FeedRow(feed: Ident, navController: NavController) {
//
//    val identViewModel = koinViewModel<IdentViewModel>()
//     Card(
//        elevation = CardDefaults.cardElevation(),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.secondaryContainer,
//            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
//        ),
//        modifier = Modifier
//            .padding(vertical = 4.dp, horizontal = 8.dp)
//            .clickable {
//                identViewModel.setupFeed(feed)
//                navController.navigate("${Scenes.FeedDetail.route}/${feed.oid}")
//            }
//    ) {
//        // private Key
//        Row(modifier = Modifier
//            .padding(12.dp,4.dp)
//            .fillMaxWidth()) {
//            Text(
//                text = feed.publicKey,
//                //color=MaterialTheme.colorScheme.primary,
//                style = keySmall,
//            )
//        }
//        // alias
//        Row(modifier = Modifier
//            .padding(start = 12.dp, bottom = 8.dp)
//            .fillMaxWidth()) {
//
//            Image(
//                modifier = Modifier
//                    .padding(top = 12.dp, end = 8.dp)
//                    .size(size = 48.dp)
//                    .clip(shape = CircleShape)
//                    .background(MaterialTheme.colorScheme.onSecondaryContainer),
//                painter = rememberVectorPainter(Icons.Rounded.PermIdentity),
//                contentDescription = "Profile Image"
//            )
//            Row(Modifier.fillMaxWidth()) {
//                Text(
//                    modifier = Modifier.padding(top = 24.dp).weight(0.85f),
//                    text = feed.alias,
//                    //color=MaterialTheme.colorScheme.primary,
//                    style = MaterialTheme.typography.headlineSmall,
//                )
//                if (feed.defaultIdent) {
//                    Badge(
//                        containerColor = MaterialTheme.colorScheme.primary,
//                        contentColor = MaterialTheme.colorScheme.onPrimary,
//                        modifier = Modifier.align(Alignment.Top).padding(end = 8.dp)
//                    ) {
//
//                        Text(
//                            "default",
//                            modifier = Modifier.semantics {
//                                contentDescription = "default"
//                            }
//                        )
//                    }
//                }
//            }
//
//        }
//        Spacer(modifier = Modifier.height(16.dp))
//    }
//}
//


//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun FeedRow(feed: Ident, navController: NavController) {
//
//    val identViewModel = koinViewModel<IdentViewModel>()
//    Card(
//        elevation = CardDefaults.cardElevation(),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.secondaryContainer,
//            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
//        ),
//        modifier = Modifier
//            .padding(vertical = 4.dp, horizontal = 8.dp)
//            .clickable {
//                identViewModel.setupFeed(feed)
//                navController.navigate("${Scenes.FeedDetail.route}/${feed.oid}")
//            }
//    ) {
//        // private Key
//        Row(modifier = Modifier
//            .padding(12.dp,4.dp)
//            .fillMaxWidth()) {
//            Text(
//                text = feed.publicKey,
//                //color=MaterialTheme.colorScheme.primary,
//                style = keySmall,
//            )
//        }
//        // alias
//        Row(modifier = Modifier
//            .padding(start = 12.dp, bottom = 8.dp)
//            .fillMaxWidth()) {
//
//            Image(
//                modifier = Modifier
//                    .padding(top = 12.dp, end = 8.dp)
//                    .size(size = 48.dp)
//                    .clip(shape = CircleShape)
//                    .background(MaterialTheme.colorScheme.onSecondaryContainer),
//                painter = rememberVectorPainter(Icons.Rounded.PermIdentity),
//                contentDescription = "Profile Image"
//            )
//            Row(Modifier.fillMaxWidth()) {
//                Text(
//                    modifier = Modifier.padding(top = 24.dp).weight(0.85f),
//                    text = feed.alias,
//                    //color=MaterialTheme.colorScheme.primary,
//                    style = MaterialTheme.typography.headlineSmall,
//                )
//                if (feed.defaultIdent) {
//                    Badge(
//                        containerColor = MaterialTheme.colorScheme.primary,
//                        contentColor = MaterialTheme.colorScheme.onPrimary,
//                        modifier = Modifier.align(Alignment.Top).padding(end = 8.dp)
//                    ) {
//
//                        Text(
//                            "default",
//                            modifier = Modifier.semantics {
//                                contentDescription = "default"
//                            }
//                        )
//                    }
//                }
//            }
//
//        }
//        Spacer(modifier = Modifier.height(16.dp))
//    }
//}
//
