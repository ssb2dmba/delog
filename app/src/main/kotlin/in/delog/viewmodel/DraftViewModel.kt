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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.delog.db.model.About
import `in`.delog.db.model.Draft
import `in`.delog.db.model.Ident
import `in`.delog.db.model.Message
import `in`.delog.db.model.MessageAndAbout
import `in`.delog.repository.DraftRepository
import `in`.delog.repository.MessageRepository
import `in`.delog.service.ssb.SsbMessageContent
import `in`.delog.service.ssb.SsbSignableMessage
import `in`.delog.service.ssb.SsbSignedMessage
import `in`.delog.service.ssb.makeHash
import `in`.delog.service.ssb.signMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


class DraftViewModel(
    var feed: Ident,
    var type: String?,
    val draftId: Long,
    var linkKey: String?,
    private val messageRepository: MessageRepository,
    private val draftRepository: DraftRepository
) : ViewModel() {


    //var draft: Draft by mutableStateOf(Draft.empty(feed.publicKey))

    private val _draft = MutableStateFlow(Draft.empty(feed.publicKey))
    val draft: StateFlow<Draft> = _draft.asStateFlow()


    var link: MessageAndAbout? by mutableStateOf(null)
    init {
        viewModelScope.launch(Dispatchers.IO) {
            if (draftId >=0) {
                var rval = draftRepository.getById(draftId)
                if (rval != null) {
                    _draft.update { rval }
                    if (_draft.value.branch != null) {
                        linkKey = draft.value.branch
                    }
                }
            } else if (!type.isNullOrBlank()){
                _draft.update { it.copy(type = type!!) }
            }
//            if (!linkKey.isNullOrBlank()) {
//                getLink(linkKey!!)
//            }

        }
    }
    fun updateDraftContentAsText(text: String) {
        _draft.update { it.copy(contentAsText = text) }
    }

    fun getLink(key: String) {
        GlobalScope.launch(Dispatchers.IO) {
            link = messageRepository.getMessageAndAbout(key)
            if (link != null) {
                _draft.update { it.copy(branch = link!!.message.key) }
                if (link!!.message.root != null) {
                    _draft.update { it.copy(root = link!!.message.root) }
                } else {
                    _draft.update { it.copy(root = link!!.message.key) }
                }
            }
        }
    }


    fun save(draft: Draft) {
        System.out.println("save!!!!!" + draft.oid)
        GlobalScope.launch(Dispatchers.IO) {
            if (draft.oid<=0) {
                var oid = draftRepository.insert(draft)
                _draft.value = draftRepository.getById(oid)
            } else {
                draftRepository.update(draft)
                _draft.value = draftRepository.getById(draft.oid)
            }
        }
    }

    fun delete(draft: Draft) {
        GlobalScope.launch(Dispatchers.IO) {
            draftRepository.deleteDraft(draft)
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

    fun publishDraft(draft: Draft, feed: Ident) {
        GlobalScope.launch(Dispatchers.IO) {
            val ssbSignableMessage = SsbSignableMessage.fromDraft(draft)
            // precise some blockchain info
            val last: Message? = messageRepository.getLastMessage(draft.author)
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
            draftRepository.deleteDraft(draft) // not in transaction but that's ok
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
    return SsbSignableMessage(
        previous = null,
        sequence = 1,
        author = draft.author,
        content = SsbMessageContent(
            draft.contentAsText,
            type = draft.type,
            root = draft.root,
            branch = draft.branch,
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