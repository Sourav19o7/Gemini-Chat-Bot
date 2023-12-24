package exali.morn.gemini.events

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri

sealed interface GeminiEvents {
    data class OnSendTextMessage(val text: String) : GeminiEvents
    data class OnSendMultiMediaMessage(val text: String, val imageBitmap: List<Bitmap>?) : GeminiEvents
    data class OnAddImage(val imageUris: List<Uri>, val context: Context) : GeminiEvents
}