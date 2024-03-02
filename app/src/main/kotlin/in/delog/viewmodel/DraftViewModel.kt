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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.delog.MainApplication
import `in`.delog.db.model.About
import `in`.delog.db.model.Draft
import `in`.delog.db.model.Ident
import `in`.delog.db.model.Message
import `in`.delog.db.model.MessageAndAbout
import `in`.delog.db.repository.BlobRepository
import `in`.delog.db.repository.DraftRepository
import `in`.delog.db.repository.MessageRepository
import `in`.delog.model.Mention
import `in`.delog.model.MessageViewData
import `in`.delog.model.SsbMessageContent
import `in`.delog.model.SsbSignableMessage
import `in`.delog.model.SsbSignedMessage
import `in`.delog.model.empty
import `in`.delog.model.toDraft
import `in`.delog.model.toMessageViewData
import `in`.delog.service.ssb.SsbService.Companion.format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class BlobItem(
    val key: String,
    val type: String,
    val size: Long,
    val uri: Uri
)

class DraftViewModel(
    var feed: Ident,
    var type: String?,
    private val draftId: Long,
    private var linkKey: String?,
    private val messageRepository: MessageRepository,
    private val draftRepository: DraftRepository,
    private val  blobRepository: BlobRepository
) : ViewModel() {

    private val _messageViewData = MutableStateFlow(MessageViewData.empty(feed.publicKey))
    val messageViewData: StateFlow<MessageViewData> = _messageViewData.asStateFlow()

    private val _link = MutableStateFlow(null as MessageAndAbout?)
    val link: StateFlow<MessageAndAbout?> = _link.asStateFlow()

    var isLoadingImage by mutableStateOf(false)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            if (draftId >=0) {
                val draft = draftRepository.getById(draftId)
                if (draft != null) {
                    _messageViewData.update { draft.toMessageViewData(format, blobRepository) }
                    if (!draft.branch.isNullOrEmpty()) {
                        // at first link key comes from navigation router
                        // in case we reopen a saved message link key comes from draft
                        linkKey = draft.branch
                    }
                }
            }

            if (!linkKey.isNullOrBlank()) {
                val parentMsg  = messageRepository.getMessageAndAbout(linkKey!!)
                _link.update { parentMsg }
                _messageViewData.update { it.copy(branch = parentMsg?.message?.key) }
                if (!parentMsg!!.message.root.isNullOrBlank()) {
                    _messageViewData.update { it.copy(root = parentMsg.message.root) }
                } else {
                    _messageViewData.update { it.copy(root = parentMsg.message.key) }
                }
                putParentInContentAsText()
            }
        }
    }


    private fun putParentInContentAsText() {
        val ssbMessageContent = SsbMessageContent.serialize(_messageViewData.value.contentAsText)
        ssbMessageContent.root = _messageViewData.value.root
        ssbMessageContent.branch = _messageViewData.value.branch
        _messageViewData.update { it.copy(contentAsText = ssbMessageContent.deserialize()) }
    }

    fun updateDraftContentAsText(text: String) {
        val ssbMessageContent = SsbMessageContent.serialize(_messageViewData.value.contentAsText)
        ssbMessageContent.text = text
        _messageViewData.update { it.copy(contentAsText =  ssbMessageContent.deserialize()) }
    }



    fun save(messageViewData: MessageViewData) {
        val draft = messageViewData.toDraft()
        viewModelScope.launch(Dispatchers.IO) {
            if (draft.oid<=0) {
                messageViewData.oid = draftRepository.insert(draft)
            } else {
                draftRepository.update(draft)
            }
        }
    }

    fun delete(messageViewData: MessageViewData) {
        viewModelScope.launch(Dispatchers.IO) {
            draftRepository.deleteDraft(messageViewData.toDraft())
        }
    }

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    private val _showPublishDialog = MutableStateFlow(false)
    val showPublishDialog: StateFlow<Boolean> = _showPublishDialog.asStateFlow()

    fun onPublishDialogClicked() {
        _showPublishDialog.value = true
    }

    fun onOpenDeleteDialogClicked() {
        _showDeleteDialog.value = true
    }

    fun onDeleteDialogDismiss() {
        _showDeleteDialog.value = false
    }

    fun onPublishDialogDismiss() {
        _showPublishDialog.value = false
    }

    fun publishDraft(messageViewData: MessageViewData, feed: Ident) {
        viewModelScope.launch(Dispatchers.IO) {
            val draft = messageViewData.toDraft()
            val ssbSignableMessage = SsbSignableMessage.fromDraft(draft)
            // precise some blockchain info
            val last: Message? = messageRepository.getLastMessage(messageViewData.author)
            if (last != null) {
                ssbSignableMessage.sequence = last.sequence + 1
            } else {
                ssbSignableMessage.sequence = 1
            }
            ssbSignableMessage.previous = last?.key
            // sign message
            ssbSignableMessage.hash = "sha256"
            val sig = ssbSignableMessage.signMessage(feed)

            // add sig & hash info
            val ssbSignedMessage = SsbSignedMessage(ssbSignableMessage, sig)
            val hash = ssbSignedMessage.makeHash()
            ssbSignedMessage.key = "%" + hash!!.bytes().toBase64String() + ".sha256"
            // translate to db model
            val message = fromSsbSignedMessage(ssbSignedMessage)
            // save message & delete draft
            messageRepository.addMessage(message)
            draftRepository.deleteDraft(draft)
        }
    }

    fun selectImage(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val author = _messageViewData.value.author
            try {

                val blob: BlobItem = blobRepository.insertOwnBlob(author, uri)
                    ?: throw Exception("unable to insert blob")
                if (_messageViewData.value.blobs.any { it.key == blob.key }) {
                    throw Exception("file is already attached to message")
                }
                _messageViewData.update { it.copy(blobs = it.blobs.plus(blob)) }
                blobsInContentAsText()
                isLoadingImage = false
            } catch (e: Exception) {
                MainApplication.toastify(e.message.toString())
            }
        }
    }

    private fun blobsInContentAsText() {
        val ssbMessageContent = SsbMessageContent.serialize(_messageViewData.value.contentAsText)
        var mentions =  arrayOf<Mention>()
        for (blob in messageViewData.value.blobs) {
            mentions = mentions.plus(Mention(blob.key, "", type=blob.type,size=blob.size))
        }
        ssbMessageContent.mentions = mentions
        _messageViewData.update { it.copy(contentAsText =  ssbMessageContent.deserialize()) }
    }

    fun unSelect(key: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val blobs =  messageViewData.value.blobs.filter { it.key != key }.toTypedArray()
            _messageViewData.update { it.copy(blobs = blobs) }
            blobRepository.deleteIfKeyUnused(key)
            blobsInContentAsText()
            isLoadingImage = false
        }
    }
}


fun fromSsbSignedMessage(ssbSignedMessage: SsbSignedMessage): Message {
    return Message(
        author = ssbSignedMessage.author,
        timestamp = ssbSignedMessage.timestamp,
        sequence = ssbSignedMessage.sequence,
        key = ssbSignedMessage.key,
        contentAsText = Json.encodeToString(ssbSignedMessage.content),
        type = ssbSignedMessage.content.type,
        previous = ssbSignedMessage.previous.toString(),
        signature = ssbSignedMessage.signature,
        root = ssbSignedMessage.content.root,
        branch = ssbSignedMessage.content.branch
    )
}

private fun SsbSignableMessage.Companion.fromDraft(draft: Draft): SsbSignableMessage {
    val ssbMessageContent: SsbMessageContent = Json.decodeFromString<SsbMessageContent>(
        SsbMessageContent.serializer(),
        draft.contentAsText
    )

    return SsbSignableMessage(
        previous = null,
        sequence = 1,
        author = draft.author,
        content = SsbMessageContent(
            text = ssbMessageContent.text,
            type = ssbMessageContent.type,
            root = ssbMessageContent.root,
            mentions = ssbMessageContent.mentions,
            branch = ssbMessageContent.branch
        ),
        timestamp = System.currentTimeMillis(),
        hash = ""
    )
}


fun SsbSignableMessage.Companion.fromAbout(about: About): SsbSignableMessage {
    return SsbSignableMessage(
        previous = null,
        sequence = 1,
        author = about.about,
        content = SsbMessageContent(
            about = about.about,
            name = about.name,
            description = about.description,
            image = about.image,
            type = "about"
        ),
        timestamp = System.currentTimeMillis(),
        hash = ""
    )
}