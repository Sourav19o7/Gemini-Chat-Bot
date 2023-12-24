package exali.morn.gemini.data

import android.graphics.Bitmap

data class ChatMessage(
    var message : String,
    val imageBitmap: List<Bitmap>?,
    val id : Int
)