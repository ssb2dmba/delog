package org.dlog.viewmodel


import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dlog.db.model.Ident
import org.dlog.repository.IdentRepository


class FeedViewModel(private val repository: IdentRepository) : ViewModel() {

    lateinit var feed: Ident

    fun onFeedSaving(feed: Ident) {
      GlobalScope.launch(Dispatchers.IO) {
        repository.update(feed)
        if (feed.defaultIdent) {
          repository.setDefault(feed)
        }
      }

    }

  fun deleteIdentity(feed: Ident) {
    GlobalScope.launch(Dispatchers.IO) {
      repository.delete(feed)
    }
  }

  private val _showDeleteDialog = MutableStateFlow(false)
  val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

  fun onOpenDeleteDialogClicked() {
    _showDeleteDialog.value = true
  }

  fun onDeleteDialogDismiss() {
    _showDeleteDialog.value = false
  }

  private val _showExportDialog = MutableStateFlow(false)
  val showExportDialog: StateFlow<Boolean> = _showExportDialog.asStateFlow()

  fun onOpenExportDialogClicked() {
    _showExportDialog.value = true
  }

  fun onExportDialogDismiss() {
    _showExportDialog.value = false
  }

  fun setupFeed(feed: Ident) {
    this.feed=feed
  }

}
