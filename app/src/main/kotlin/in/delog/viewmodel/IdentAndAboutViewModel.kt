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


import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.delog.MainApplication
import `in`.delog.db.model.About
import `in`.delog.db.model.Ident
import `in`.delog.db.model.IdentAndAbout
import `in`.delog.db.model.IdentAndAboutWithBlob
import `in`.delog.db.model.Message
import `in`.delog.db.repository.AboutRepository
import `in`.delog.db.repository.BlobRepository
import `in`.delog.db.repository.ContactRepository
import `in`.delog.db.repository.IdentRepository
import `in`.delog.db.repository.MessageRepository
import `in`.delog.model.SsbSignableMessage
import `in`.delog.model.SsbSignedMessage
import `in`.delog.repository.DidRepository
import `in`.delog.service.ssb.SsbService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class AboutUIState(
    val dirty: Boolean = false,
    val didValid: Boolean? = null,
    val showExportDialogState: Boolean = false,
    val showPublishDialogState: Boolean = false,
    val showDeleteDialogState: Boolean = false,
    val didValidationErrorMessage: String = "",
    val didLoading : Boolean = false,
    var identAndAboutWithBlob: IdentAndAboutWithBlob,
    var imageToPick: Uri? = null
)

class IdentAndAboutViewModel(
    private var pubKey: String,
    private val identRepository: IdentRepository,
    private val aboutRepository: AboutRepository,
    private val messageRepository: MessageRepository,
    private val didRepository: DidRepository,
    private val blobRepository: BlobRepository,
    private val contactRepository: ContactRepository
) : ViewModel() {

    private var _uiState: MutableStateFlow<AboutUIState?> = MutableStateFlow(null)
    var uiState: StateFlow<AboutUIState?> = _uiState.asStateFlow()
    private var _redirect: MutableStateFlow<Ident?> = MutableStateFlow(null)
    var redirect: StateFlow<Ident?> = _redirect.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = AboutUIState(identAndAboutWithBlob = identRepository.findById(pubKey))
        }
    }

    fun update(input: AboutUIState) {
        if(_uiState.value==null) return
        _uiState.value = input
        if (input.imageToPick!=null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val blob =
                        blobRepository.insertOwnBlob(input.identAndAboutWithBlob.about.about, input.imageToPick!!)
                            ?: throw Exception("unable to insert blob")
                    if (_uiState.value!!.identAndAboutWithBlob.about.image == blob.key) {
                        throw Exception("file is already attached to message")
                    }
                    val newState = _uiState.value!!.copy()
                    newState.identAndAboutWithBlob.about.image = blob.key
                    newState.identAndAboutWithBlob.profileImage = blob.uri
                    newState.imageToPick = null
                    _uiState.value = newState
                } catch (e: Exception) {
                    MainApplication.toastify(e.message.toString())
                }
            }
        }
    }



    fun onSavingIdent(ident: Ident) {
        viewModelScope.launch(Dispatchers.IO) {
            identRepository.update(ident)
            if (ident.defaultIdent) {
                identRepository.setFeedAsDefaultFeed(ident)
            }
            setDirty(false)
        }
    }

    fun redeemInvite(ident: Ident) {
        viewModelScope.launch {
            var ssbService = SsbService(
                messageRepository,
                aboutRepository,
                contactRepository,
                blobRepository
            )
            ssbService.connectWithInvite(ident,
                {
                    // everything is going according to the plan
                    MainApplication.toastify("Identity has been successfully validated on the relay.")
                    _redirect.value = ident
                },
                {
                    MainApplication.toastify(it.message.toString())
                    _redirect.value = ident
                })
        }
    }

    fun onSavingAbout(about: About) {
        viewModelScope.launch(Dispatchers.IO) {
            aboutRepository.insertOrUpdate(about)
            setDirty(true)
        }
    }

    fun cleanInvite(newIdent: Ident) {
        viewModelScope.launch(Dispatchers.IO) {
            identRepository.cleanInvite(newIdent)
        }
    }

    fun delete(ident: Ident) {
        viewModelScope.launch(Dispatchers.IO) {
            identRepository.delete(ident)
        }
    }

    fun onDoPublishClicked(about: About) {
        viewModelScope.launch(Dispatchers.IO) {
            val iAndA: IdentAndAbout = identRepository.findByPublicKey(about.about) ?: return@launch
            val ident = iAndA.ident
            val ssbSignableMessage = SsbSignableMessage.fromAbout(about)
            // precise some blockchain info
            val last: Message? = messageRepository.getLastMessage(about.about)
            if (last != null) {
                ssbSignableMessage.sequence = last.sequence + 1
            } else {
                ssbSignableMessage.sequence = 1
            }
            ssbSignableMessage.content.type = "about"
            ssbSignableMessage.content.image = iAndA.about!!.image
            ssbSignableMessage.content.description = iAndA.about!!.description
            ssbSignableMessage.previous = last?.key
            // sign message
            ssbSignableMessage.hash = "sha256"
            val sig = ssbSignableMessage.signMessage(ident)

            // add sig & hash info
            val ssbSignedMessage = SsbSignedMessage(ssbSignableMessage, sig)
            val hash = ssbSignedMessage.makeHash()
            ssbSignedMessage.key = "%" + hash!!.bytes().toBase64String() + ".sha256"
            // translate to db model
            val message = fromSsbSignedMessage(ssbSignedMessage)
            // save message & delete draft
            messageRepository.addMessage(message)
            aboutRepository.insertOrUpdate(about = about)
        }

    }


    fun setDirty(b: Boolean) {
        _uiState.update { it!!.copy(dirty = b) }
    }



    private fun checkName(identAndAbout: IdentAndAbout?, newValue: String) {
        _uiState.update { it!!.copy(didLoading = true) }
        viewModelScope.launch {
            val response = didRepository.checkIfValid(identAndAbout, newValue)
            if (!response.valid) {
                _uiState.update { it!!.copy(didValidationErrorMessage = response.error ?:"", didValid = false, didLoading = false) }
            } else {
                _uiState.update { it!!.copy(didValidationErrorMessage = "", didValid = true, didLoading = false) }
            }
        }
    }


    // Dialogs
    fun closeExportDialog() {
        _uiState.update { it!!.copy(showExportDialogState = false) }
    }

    fun closePublishDialog() {
        _uiState.update { it!!.copy(showPublishDialogState = false) }
    }

    fun closeDeteDialog() {
        _uiState.update { it!!.copy(showDeleteDialogState = false) }
    }


    fun openExportDialog() {
        _uiState.update { it!!.copy(showExportDialogState = true) }
    }

    fun openDeleteDialog() {
        _uiState.update { it!!.copy(showDeleteDialogState = true) }
    }

    fun showPublishDialog() {
        _uiState.update { it!!.copy(showPublishDialogState = true) }
    }

}
