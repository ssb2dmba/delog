package `in`.delog.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.util.Consumer
import androidx.emoji2.emojipicker.EmojiPickerView
import androidx.emoji2.emojipicker.EmojiViewItem


@Composable
fun EmojiPicker(callback: Consumer<EmojiViewItem>?) {
    AndroidView(
        factory = { context ->
            var emoji = EmojiPickerView(context)
            emoji.setOnEmojiPickedListener(callback)
            emoji
        },
        modifier = Modifier.fillMaxWidth(),
    )
}