package com.michaeo04.spendlikeamillionaire.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.michaeo04.spendlikeamillionaire.platform.ImageCache

/** Item photo when one is bundled, otherwise the emoji icon. Size/shape come from [modifier]. */
@Composable
fun ItemImage(
    icon: String,
    image: String?,
    modifier: Modifier = Modifier,
    emojiSize: TextUnit = 56.sp,
) {
    val assets = LocalContext.current.assets
    val bitmap by produceState<ImageBitmap?>(initialValue = image?.let(ImageCache::peek), key1 = image) {
        value = image?.let { ImageCache.load(assets, it) }
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        val loaded = bitmap
        if (loaded != null) {
            Image(
                bitmap = loaded,
                contentDescription = null, // the item name next to it already describes it
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(icon, fontSize = emojiSize)
        }
    }
}
