package org.dlog.ui.component


data class MessageUIModel(

    val feed: Int,

    val key: String,

    val timestamp: Long,

    val author: String,

    val contentAsText: String,

    )