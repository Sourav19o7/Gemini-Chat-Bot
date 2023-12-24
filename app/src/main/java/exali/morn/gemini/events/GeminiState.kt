package exali.morn.gemini.events

import android.graphics.Bitmap
import exali.morn.gemini.data.ChatMessage

data class GeminiState(
    val chatMessages: List<ChatMessage> = emptyList(),
    val imageBitmap: List<Bitmap>? = null,
    val geminiResponse: String = "",
    val loading : Boolean = false
)