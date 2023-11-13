package `in`.delog.ui.component.richtext

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
fun checkForHashtagWithIcon(tag: String, primary: Color): HashtagIcon? {
    return when (tag.lowercase()) {
        //"bitcoin", "btc", "timechain", "bitcoiner", "bitcoiners" -> HashtagIcon(Icons.Default.CurrencyBitcoin, "Bitcoin", Color.Unspecified, Modifier.padding(2.dp, 2.dp, 0.dp, 0.dp))
        //"coffee", "coffeechain", "cafe" -> HashtagIcon(R.drawable.coffee, "Coffee", Color.Unspecified, Modifier.padding(2.dp, 2.dp, 0.dp, 0.dp))
        else -> null
    }
}

@Immutable
class HashtagIcon(
    val icon: Int,
    val description: String,
    val color: Color,
    val modifier: Modifier
)