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
import `in`.delog.db.model.*
import `in`.delog.repository.DraftRepository
import `in`.delog.repository.MessageRepository
import `in`.delog.ssb.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


class DraftViewModel(
    var feed: Ident,
    private val messageRepository: MessageRepository,
    private val draftRepository: DraftRepository
) : ViewModel() {

    //signal your view when the coroutine finishes insert
    var inserted: Long? by mutableStateOf(null)

    var draft: Draft? by mutableStateOf(null)

    var link: MessageAndAbout? by mutableStateOf(null)

    fun getLink(key: String) {
        GlobalScope.launch(Dispatchers.IO) {
            link = messageRepository.getMessageAndAbout(key)
        }
    }

    fun insert(draft: Draft) {
        GlobalScope.launch(Dispatchers.IO) {
            inserted = draftRepository.insert(draft)
            setCurrentDraft(inserted.toString())
        }
    }



    fun setCurrentDraft(oid: String) {
        GlobalScope.launch(Dispatchers.IO) {
            draft = draftRepository.getById(oid.toInt())
        }
    }

    fun update(draft: Draft) {
        GlobalScope.launch(Dispatchers.IO) {
            draftRepository.update(draft)
        }
    }

    fun deleteDraft(draft: Draft) {
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
            var last: Message? = messageRepository.getLastMessage(draft.author)
            if (last != null) {
                ssbSignableMessage.sequence = last.sequence + 1
            } else {
                ssbSignableMessage.sequence = 1
            }
            ssbSignableMessage.previous = last?.key;
            // sign message
            ssbSignableMessage.hash = "sha256";
            var sig = ssbSignableMessage.signMessage(feed)

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