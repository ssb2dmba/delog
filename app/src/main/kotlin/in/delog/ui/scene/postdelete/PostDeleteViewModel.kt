package `in`.delog.ui.scene.postdelete

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.delog.db.model.Message
import `in`.delog.db.repository.IdentRepository
import `in`.delog.db.repository.MessageRepository
import `in`.delog.service.ssb.SsbService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MessageDeleteViewModel(
private var key: String,
private val ssbService: SsbService,
private val messageRepository: MessageRepository,
private val identRepository: IdentRepository,
) : ViewModel() {

    private val _messageEntity = MutableStateFlow(null as Message?)
    var messageEntity: StateFlow<Message?> = _messageEntity.asStateFlow()

    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            var m = messageRepository.getMessage(key)
            _messageEntity.update { m }
        }
    }

    fun deletePost() {
        viewModelScope.launch(Dispatchers.IO) {
            if (_messageEntity.value == null) {
                Log.w(SsbService.TAG, "Message not found while calling delete post")
                return@launch
            }
            val feed = identRepository.findByPublicKey(_messageEntity.value!!.author!!)
            if (feed == null) {
                Log.w(SsbService.TAG, "Feed not found while calling delete post")
                return@launch
            }
            ssbService.deletePost(feed.ident, _messageEntity.value)
            _done.update { true }
        }
    }
}