package exali.morn.gemini.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import dagger.hilt.android.lifecycle.HiltViewModel
import exali.morn.gemini.Constants.GEMINI_API_KEY
import exali.morn.gemini.Constants.GEMINI_PRO
import exali.morn.gemini.Constants.GEMINI_PRO_VERSION
import exali.morn.gemini.data.ChatMessage
import exali.morn.gemini.domain.GeminiRepository
import exali.morn.gemini.events.GeminiEvents
import exali.morn.gemini.events.GeminiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.FileDescriptor
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class GeminiViewModel @Inject constructor(
    val geminiRepository: GeminiRepository
) : ViewModel(
) {
    private val _geminiState = MutableStateFlow(GeminiState())
    val geminiState = _geminiState.asStateFlow()


    //TODO : Add creativity with generation configs
    val generativeModelPro = GenerativeModel(
        // For text-and-image input (multimodal), use the gemini-pro-vision model
        modelName = GEMINI_PRO_VERSION,
        // Access your API key as a Build Configuration variable (see "Set up your API key" above)
        apiKey = GEMINI_API_KEY,
    )

    val generativeModel = GenerativeModel(
        // For text-only input, use the gemini-vision model
        modelName = GEMINI_PRO,
        // Access your API key as a Build Configuration variable (see "Set up your API key" above)
        apiKey = GEMINI_API_KEY,
    )

    fun geminiAction(event: GeminiEvents) {
        when (event) {
            is GeminiEvents.OnSendTextMessage -> {
                _geminiState.update {
                    it.copy(
                        loading = true,
                        geminiResponse = "",
                        chatMessages = it.chatMessages + ChatMessage(
                            message = event.text, id = 2, imageBitmap = null
                        ) + ChatMessage(
                            message = "", id = 1, imageBitmap = null
                        )
                    )
                }

                viewModelScope.launch {
                    try {
                        generativeModel.generateContentStream(event.text).collect { chunk ->
                            Log.d("Gemini REsponse", chunk.text.toString())
                            _geminiState.update {
                                val newMessageList = it.chatMessages.toMutableList()
                                newMessageList[newMessageList.size - 1].message += chunk.text
                                it.copy(
                                    loading = false,
                                    chatMessages = newMessageList,
                                    geminiResponse = it.geminiResponse + chunk.text
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("Gemini", e.message.toString())
                    }
                }
            }

            is GeminiEvents.OnSendMultiMediaMessage -> {
                val image = event.imageBitmap
                _geminiState.update {
                    it.copy(
                        loading = true,
                        geminiResponse = "",
                        chatMessages = it.chatMessages + ChatMessage(
                            message = event.text, id = 2, imageBitmap = image
                        ) + ChatMessage(
                            message = "", id = 1, imageBitmap = null
                        ),
                        imageBitmap = null
                    )
                }

                viewModelScope.launch {
                    try {
                        val inputContent = content {
                            image!!.forEach {
                                image(it)
                            }
                            text(event.text)
                        }
                        generativeModelPro.generateContentStream(inputContent).collect { chunk ->
                            _geminiState.update {
                                val newMessageList = it.chatMessages.toMutableList()
                                newMessageList[newMessageList.size - 1].message += chunk.text
                                it.copy(
                                    loading = false,
                                    chatMessages = newMessageList,
                                    geminiResponse = it.geminiResponse + chunk.text
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("Gemini issue", "Error $e")
                    }
                }
            }

            is GeminiEvents.OnAddImage -> {
                var imageBitmaps = listOf<Bitmap>()
                event.imageUris.forEach {
                    imageBitmaps = (imageBitmaps + getBitmapFromUri(it, event.context)!!)
                }
                _geminiState.update {
                    it.copy(
                        imageBitmap = imageBitmaps
                    )
                }
            }
        }
    }

    private fun getBitmapFromUri(imageUri: Uri, context: Context): Bitmap? {
        try {
            val parcelFileDescriptor = context.contentResolver.openFileDescriptor(imageUri, "r")
            val fileDescriptor: FileDescriptor = parcelFileDescriptor!!.fileDescriptor
            val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            parcelFileDescriptor.close()
            return image
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }
}