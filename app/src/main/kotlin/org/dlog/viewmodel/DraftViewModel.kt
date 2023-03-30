package org.dlog.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.dlog.db.model.Draft
import org.dlog.repository.DraftRepository

class DraftNewViewModel(private val feedOid: Int, private val repository: DraftRepository) : ViewModel() {

    fun insert(draft: Draft) {
        GlobalScope.launch(Dispatchers.IO) {
            repository.insert(draft)
        }
    }

}
