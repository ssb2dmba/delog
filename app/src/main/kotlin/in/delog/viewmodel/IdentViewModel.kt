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
import `in`.delog.db.model.About
import `in`.delog.db.model.Ident
import `in`.delog.db.model.Message
import `in`.delog.repository.AboutRepository
import `in`.delog.repository.IdentRepository
import `in`.delog.repository.MessageRepository
import `in`.delog.ssb.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class IdentViewModel(
    private val identRepository: IdentRepository,
    private val aboutRepository: AboutRepository,
    private val messageRepository: MessageRepository
) : ViewModel() {

    var ident: Ident? by mutableStateOf(null)
    var about: About? by mutableStateOf(null)
    var dirty: Boolean by mutableStateOf(false)

    private val _showExportDialog = MutableStateFlow(false)
    val showExportDialog: StateFlow<Boolean> = _showExportDialog.asStateFlow()

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    private val _showPublishDialog = MutableStateFlow(false)
    val showPublishDialog: StateFlow<Boolean> = _showPublishDialog.asStateFlow()

    fun setCurrentIdent(oid: String) {
        GlobalScope.launch(Dispatchers.IO) {
            var identAndAbout = identRepository.findById(oid)
            if (identAndAbout == null) return@launch
            ident = identAndAbout.ident
            if (ident != null && identAndAbout.about == null) {
                // each ident shall have about
                val newAbout = About(ident!!.publicKey, ident!!.alias, "", null, dirty = false)
                aboutRepository.insert(newAbout)
                about = newAbout
            } else {
                about = identAndAbout.about
            }
            dirty = false
        }
    }


    fun setCurrentIdentByPk(oid: String) {
        GlobalScope.launch(Dispatchers.IO) {
            var identAndAbout = identRepository.findByPublicKey(oid)
            if (identAndAbout != null) {
                ident = identAndAbout.ident
                if (identAndAbout.about == null) {
                    // each ident shall have about
                    val newAbout = About(ident!!.publicKey, ident!!.alias, "", null, dirty = false)
                    aboutRepository.insert(newAbout)
                    about = newAbout
                } else {
                    about = identAndAbout.about
                }
                dirty = false
            } else {
                Log.e("dlog", "ident repository find by public key retunn nul !")
            }
        }
    }

    fun onSavingIdent(ident: Ident) {
        GlobalScope.launch(Dispatchers.IO) {
            identRepository.update(ident)
            if (ident.defaultIdent) {
                identRepository.setDefault(ident)
            }
            dirty = false
        }
    }


    fun onSavingAbout(about: About) {
        GlobalScope.launch(Dispatchers.IO) {
            aboutRepository.insertOrUpdate(about)
            dirty = false
        }
        this.about = about
    }


    fun delete(ident: Ident) {
        GlobalScope.launch(Dispatchers.IO) {
            identRepository.delete(ident)
        }
    }


    fun onOpenDeleteDialogClicked() {
        _showDeleteDialog.value = true
    }

    fun onDeleteDialogDismiss() {
        _showDeleteDialog.value = false
    }

    fun onOpenExportDialogClicked() {
        _showExportDialog.value = true
    }

    fun onExportDialogDismiss() {
        _showExportDialog.value = false
    }

    fun onOpenPublishDialogClicked() {
        _showPublishDialog.value = true
    }

    fun onPublishDialogDismiss() {
        _showPublishDialog.value = false
    }

    fun onDoPublishClicked(_about: About) {
        this.about = _about
        if (this.about == null) return
        GlobalScope.launch(Dispatchers.IO) {
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

    var connecting = false;

    fun connectWithInvite(ident: Ident, toCanonicalForm: String, ssbService: SsbService) {
        if (connecting) return
        ssbService.rpcHandler?.close()
        viewModelScope.launch {
            connecting = true
            try {
                ssbService.connectWithInvite(toCanonicalForm, ident, {
                    // TODO follow up on implementic connect with invite
                })
            } catch (e: Exception) {
                e.printStackTrace();
            }
        }

    }

}
