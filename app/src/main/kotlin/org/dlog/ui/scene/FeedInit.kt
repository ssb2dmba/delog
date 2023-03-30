package org.dlog.scene

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import org.apache.tuweni.scuttlebutt.Invite
import org.dlog.db.model.Ident
import org.dlog.ssb.SsbService
import org.dlog.viewmodel.IdentViewModel
import org.koin.androidx.compose.get
import org.koin.androidx.compose.koinViewModel
import org.koin.core.definition.indexKey
import org.koin.core.parameter.parametersOf


@Composable
fun FeedInit(navController: NavController, id: String) {
    val vm = koinViewModel<IdentViewModel>(parameters = { parametersOf(id) })
    LaunchedEffect(id) {
        id?.let { vm.setCurrentIdent(it) }
    }
    if (vm.ident==null) {
        return
    }
    val ssbService: SsbService = get()
    System.out.println(vm.ident!!.invite)
    //vm.ident!!.invite="192.168.0.45:8008:@YpSbE5/7oWuf7k6zhU/wwbm28EffUggYEwVpDkOAdIg=.ed25519~eyLhxIFakSVJg9e6okzmT4r6tSx5HDcraGTAfM57SO8="
    vm.ident!!.invite?.let { vm.connectWithInvite(vm.ident!!, it, ssbService) }

    Text(text="feed init done")
}




