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


import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.delog.db.model.*
import `in`.delog.repository.AboutRepository
import `in`.delog.repository.DidRepository
import `in`.delog.repository.IdentRepository
import `in`.delog.repository.MessageRepository
import `in`.delog.service.ssb.SsbSignableMessage
import `in`.delog.service.ssb.SsbSignedMessage
import `in`.delog.service.ssb.makeHash
import `in`.delog.service.ssb.signMessage
import `in`.delog.service.ssb.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Immutable
data class AboutUIState(
    val identAndAbout: IdentAndAbout? = null,
    val dirty: Boolean = false,
    val didValid: Boolean? = null,
    val showExportDialogState: Boolean = false,
    val showPublishDialogState: Boolean = false,
    val showDeleteDialogState: Boolean = false,
    val image: String = "",
    val error: String = "",
    val alias: String = "",
    val description: String = ""
)

@OptIn(ExperimentalCoroutinesApi::class)
class IdentAndAboutViewModel(
    private var pubKey: String,
    private val identRepository: IdentRepository,
    private val aboutRepository: AboutRepository,
    private val messageRepository: MessageRepository,
    private val didRepository: DidRepository,
) : ViewModel() {

    val _uiState: MutableStateFlow<AboutUIState> = MutableStateFlow(AboutUIState())
    val uiState: StateFlow<AboutUIState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val iAndA = identRepository.findById(pubKey)
            _uiState.update {
                it.copy(
                    identAndAbout = iAndA,
                    alias = iAndA.about?.name ?: "",
                    description = iAndA.about?.description ?: ""
                )
            }
            uiState.value.identAndAbout?.about?.name?.let {
                checkName(
                    uiState.value.identAndAbout,
                    it
                )
            }
        }
    }

    fun onSavingIdent(ident: Ident) {
        GlobalScope.launch(Dispatchers.IO) {
            identRepository.update(ident)
            if (ident.defaultIdent) {
                identRepository.setFeedAsDefaultFeed(ident)
            }
            setDirty(false)
        }
    }

    fun onSavingAbout(about: About) {
        GlobalScope.launch(Dispatchers.IO) {
            aboutRepository.insertOrUpdate(about)
            setDirty(true)
        }
    }

    fun cleanInvite(newIdent: Ident) {
        GlobalScope.launch(Dispatchers.IO) {
            identRepository.cleanInvite(newIdent)
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


    fun setDirty(b: Boolean) {
        _uiState.update { it.copy(dirty = b) }
    }

    // text fields
    fun updateAlias(newValue: String) {
        _uiState.update { newUiState ->
            checkName(uiState.value.identAndAbout, newValue)
            newUiState.copy(
                alias = newValue.filter { it.isLetter() }.lowercase().trim()
            )

        }
    }

    private fun checkName(identAndAbout: IdentAndAbout?, newValue: String) {
        viewModelScope.launch {
            val r = didRepository.checkIfValid(identAndAbout, newValue)
            if (r.error != null) {
                _uiState.update { it.copy(error = r.error) }
            }
            _uiState.update { it.copy(didValid = r.valid) }
        }
    }

    val aliasHasError: StateFlow<Boolean> =
        snapshotFlow { uiState.value.alias }
            .mapLatest {
                val r = didRepository.checkIfValid(uiState.value.identAndAbout, it)
                if (r.error != null) {
                    _uiState.update { it.copy(error = r.error) }
                }
                !r.valid
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = false
            )

    fun updateDescription(value: String) {
        _uiState.update { it.copy(description = value.trim()) }
    }

    // Dialogs
    fun closeExportDialog() {
        _uiState.update { it.copy(showExportDialogState = false) }
    }

    fun closePublishDialog() {
        _uiState.update { it.copy(showPublishDialogState = false) }
    }

    fun closeDeteDialog() {
        _uiState.update { it.copy(showDeleteDialogState = false) }
    }


    fun openExportDialog() {
        _uiState.update { it.copy(showExportDialogState = true) }
    }

    fun openDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialogState = true) }
    }

    fun showPublishDialog() {
        _uiState.update { it.copy(showPublishDialogState = true) }
    }
}
