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
package `in`.delog.viewmodel


import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.delog.db.model.*
import `in`.delog.repository.AboutRepository
import `in`.delog.repository.IdentRepository
import `in`.delog.repository.MessageRepository
import `in`.delog.ssb.*
import `in`.delog.ssb.BaseSsbService.Companion.TAG
import `in`.delog.ssb.BaseSsbService.Companion.objectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.apache.tuweni.scuttlebutt.lib.model.FeedMessage
import org.apache.tuweni.scuttlebutt.lib.model.toAbout
import org.apache.tuweni.scuttlebutt.rpc.RPCResponse
import java.net.URL
import kotlin.concurrent.thread


class IdentAndAboutViewModel(
    private val identRepository: IdentRepository,
    private val aboutRepository: AboutRepository,
    private val messageRepository: MessageRepository,
    private val ssbService: SsbService
) : ViewModel() {

    var identAndAbout: IdentAndAbout? by mutableStateOf(null)

    var dirty: Boolean by mutableStateOf(false)

    var _networkIdentifierValid: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    var networkIdentifierValid: StateFlow<Boolean?> = _networkIdentifierValid.asStateFlow()

    fun checkIfValid(name: String?) {
        if (name.isNullOrEmpty() || identAndAbout?.about == null) return
        var httpScheme = identAndAbout!!.ident.getHttpScheme()
        val server = identAndAbout!!.ident.server
        thread {
            val jsonResponse: String = try {
                val url = "${httpScheme}${server}/.well-known/ssb/about/${name}"
                URL(url).readText()
            } catch (e: Exception) {
                Log.e(TAG, e.javaClass.simpleName + " " + e.localizedMessage)
                return@thread
            }
            Log.d(TAG,jsonResponse)
            try {
                val m: FeedMessage =
                    objectMapper.readerFor(FeedMessage::class.java).readValue(jsonResponse)
                val about = m.toAbout();
                _networkIdentifierValid.value =
                    about != null && (about.about == "AVAILABLE" || about.about == identAndAbout!!.ident.publicKey)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _networkIdentifierValid.value = false
        }
    }

    fun setCurrentIdent(oid: String) {
        GlobalScope.launch(Dispatchers.IO) {
            identAndAbout = identRepository.findById(oid)
            if (identAndAbout == null) return@launch
            if (identAndAbout!!.about == null) {
                // each ident shall have about
                val newAbout = About(identAndAbout!!.ident.publicKey, "", "", null, dirty = false)
                aboutRepository.insert(newAbout)
                identAndAbout!!.about = newAbout
            }
            dirty = false
        }
    }

    fun setCurrentIdentByPk(pk: String) {
        GlobalScope.launch(Dispatchers.IO) {
            identAndAbout = identRepository.findByPublicKey(pk)
            if (identAndAbout == null) return@launch
            if (identAndAbout!!.about == null) {
                // each ident shall have about
                val newAbout = About(identAndAbout!!.ident.publicKey, "", "", null, dirty = false)
                aboutRepository.insert(newAbout)
                identAndAbout!!.about = newAbout
            }
            dirty = false
        }
    }

    fun onSavingIdent(ident: Ident) {
        GlobalScope.launch(Dispatchers.IO) {
            identRepository.update(ident)
            if (ident.defaultIdent) {
                identRepository.setFeedAsDefaultFeed(ident)
            }
            dirty = false
        }
    }


    fun onSavingAbout(about: About) {
        GlobalScope.launch(Dispatchers.IO) {
            aboutRepository.insertOrUpdate(about)
            dirty = false
        }

    }

    fun delete(ident: Ident) {
        GlobalScope.launch(Dispatchers.IO) {
            identRepository.delete(ident)
        }
    }

    fun onDoPublishClicked(about: About) {
        GlobalScope.launch(Dispatchers.IO) {
            val iAndA: IdentAndAbout = identRepository.findByPublicKey(about.about)
            if (iAndA == null) {
                return@launch
            }
            val ident = iAndA.ident
            val ssbSignableMessage = SsbSignableMessage.fromAbout(about!!)
            // precise some blockchain info
            var last: Message? = messageRepository.getLastMessage(about!!.about)
            if (last != null) {
                ssbSignableMessage.sequence = last.sequence + 1
            } else {
                ssbSignableMessage.sequence = 1
            }
            ssbSignableMessage.content.type = "about"
            ssbSignableMessage.previous = last?.key;
            // sign message
            ssbSignableMessage.hash = "sha256";
            var sig = ssbSignableMessage.signMessage(ident!!)

            // add sig & hash info
            val ssbSignedMessage = SsbSignedMessage(ssbSignableMessage, sig)
            val hash = ssbSignedMessage.makeHash()
            ssbSignedMessage.key = "%" + hash!!.bytes().toBase64String() + ".sha256"
            // translate to db model
            val message = fromSsbSignedMessage(ssbSignedMessage)
            // save message & delete draft
            messageRepository.addMessage(message)
            aboutRepository.insertOrUpdate(about = about!!)
        }

    }

    var connecting = false

    var networkError: String? = null
    fun connectWithInvite(ident: Ident, callBack: (RPCResponse) -> Unit) {
        if (connecting) return

        ssbService.rpcHandler?.close()

        viewModelScope.launch {
            connecting = true
            try {
                ssbService.connectWithInvite(ident, callBack)
            } catch (e: Exception) {
                networkError = e.message
                e.printStackTrace();
                connecting = false
            }
        }
    }

    fun cleanInvite(newIdent: Ident) {
        GlobalScope.launch(Dispatchers.IO) {
            identRepository.cleanInvite(newIdent)
        }
    }
}
