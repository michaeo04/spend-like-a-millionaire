package com.michaeo04.spendlikeamillionaire.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.michaeo04.spendlikeamillionaire.platform.ImageCache

/** Round portrait when one is bundled, otherwise a colored disc with the person's initial. */
@Composable
fun Avatar(
    name: String,
    colorHex: String,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
    image: String? = null,
) {
    val color = runCatching { Color(android.graphics.Color.parseColor(colorHex)) }
        .getOrDefault(Color(0xFF5B8DEF))
    val assets = LocalContext.current.assets
    val photo by produceState<ImageBitmap?>(initialValue = image?.let(ImageCache::peek), key1 = image) {
        value = image?.let { ImageCache.load(assets, it) }
    }
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center,
    ) {
        val loaded = photo
        if (loaded != null) {
            Image(
                bitmap = loaded,
                contentDescription = null, // the name is shown next to the avatar
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = name.trim().take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.45f).sp,
            )
        }
    }
}
