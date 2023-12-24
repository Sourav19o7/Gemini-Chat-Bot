package exali.morn.gemini

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import exali.morn.gemini.data.ChatMessage
import exali.morn.gemini.events.GeminiEvents
import exali.morn.gemini.ui.GeminiViewModel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            val viewModel: GeminiViewModel = hiltViewModel()
            val state by viewModel.geminiState.collectAsState()
            val actions = viewModel::geminiAction
            val context = LocalContext.current

            val launcher = rememberLauncherForActivityResult(
                contract =
                ActivityResultContracts.GetMultipleContents()
            ) { list: List<Uri> ->
                actions(GeminiEvents.OnAddImage(list, context))
            }

            ChatScreen(
                loading = state.loading,
                geminiResponse = state.geminiResponse,
                imageBitmap = state.imageBitmap,
                chatMessages = state.chatMessages,
                onAddImageClick = {
                    launcher.launch("image/*")
                },
                onSendClick = { newMessage ->
                    if (state.imageBitmap == null) {
                        actions(GeminiEvents.OnSendTextMessage(newMessage))
                    } else {
                        actions(GeminiEvents.OnSendMultiMediaMessage(newMessage, state.imageBitmap))
                    }
                })
        }
    }
}

@Preview
@Composable
fun ChatScreen(
    loading: Boolean = false,
    geminiResponse: String = "",
    imageBitmap: List<Bitmap>? = null,
    chatMessages: List<ChatMessage> = listOf(),
    onSendClick: (String) -> Unit = {},
    onAddImageClick: () -> Unit = {}
) {


    Box {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            GeminiToolbar(loading = loading)
            Box(modifier = Modifier.weight(1f)) {
                ChatMessageList(
                    geminiResponse = geminiResponse,
                    messages = chatMessages
                )
            }
            ChatInputBox(
                loading = loading,
                imageBitmap = imageBitmap,
                chatMessages = chatMessages,
                onSendClick = onSendClick,
                onAddImageClick = onAddImageClick,
            )

        }

    }

}

@Composable
fun ChatInputBox(
    loading: Boolean = false,
    imageBitmap: List<Bitmap>? = null,
    chatMessages: List<ChatMessage>,
    onSendClick: (String) -> Unit = {},
    onAddImageClick: () -> Unit = {}
) {
    var newMessage by remember { mutableStateOf("") }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        ChatInputField(
            loading = loading,
            imageBitmap = imageBitmap,
            message = newMessage,
            onMessageChange = {
                newMessage = it
            },
            onSendClick = {
                if (newMessage.isNotEmpty()) {
                    chatMessages.plus(newMessage)
                    onSendClick(newMessage)
                    newMessage = ""
                }
            }
        ) {
            onAddImageClick()
        }
    }
}

@Composable
fun GeminiToolbar(
    loading: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(id = R.color.grey))
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.clip(shape = CircleShape)) {
            Image(
                modifier = Modifier.size(50.dp),
                painter = painterResource(id = R.drawable.gemini_embassador),
                contentDescription = "Geminis Avatar"
            )
        }

        Column {
            Text(
                text = "Gemini", color = Color.White, style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = if (loading) "typing..." else "Online",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

}

@Composable
fun ChatMessageList(
    geminiResponse: String,
    messages: List<ChatMessage> = listOf()
) {
    val lazyColumnListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .background(Color.Black)
            .fillMaxSize()
    ) {
        Image(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            contentScale = ContentScale.Crop,
            painter = painterResource(id = R.drawable.gemini_logo),
            contentDescription = null
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(),
            state = lazyColumnListState
        ) {
            coroutineScope.launch {
                if (messages.isNotEmpty()) {
                    lazyColumnListState.scrollToItem(messages.size - 1)
                }
            }
            items(messages) { message ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (message.id == 1) Alignment.CenterStart
                    else Alignment.CenterEnd
                ) {
                    ChatMessage(
                        chatMessage = if (message.id == 1 && message.message == "")
                            ChatMessage(
                                message = geminiResponse,
                                id = 1,
                                imageBitmap = null
                            ) else message
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun ChatMessage(
    chatMessage: ChatMessage = ChatMessage(
        message = "Hello, I am Gemini!",
        id = 1,
        imageBitmap = null
    )
) {
    if (chatMessage.message != "") {
        Column(
            modifier = Modifier
                .padding(15.dp),
            horizontalAlignment = if (chatMessage.id == 1)
                Alignment.Start
            else
                Alignment.End,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (chatMessage.imageBitmap != null) {
                chatMessage.imageBitmap.forEach {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .width(270.dp)
                            .height(400.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = 1.dp,
                                color = Color.White,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .background(colorResource(id = R.color.grey))
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .widthIn(max = 270.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        width = 1.dp, color = Color.Transparent, shape = RoundedCornerShape(40.dp)
                    )
                    .background(colorResource(id = R.color.grey))

            ) {
                Text(
                    text = chatMessage.message,
                    modifier = Modifier.padding(18.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium

                )
            }
        }
    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ChatInputField(
    loading: Boolean = false,
    imageBitmap: List<Bitmap>? = null,
    message: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAddImageClick: () -> Unit = {}
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (imageBitmap != null){
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            imageBitmap!!.forEach {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            width = 1.dp,
                            color = Color.White,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .background(colorResource(id = R.color.grey))
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                )
            }
        }}


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 55.dp)
                .background(Color.Black)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextField(colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = colorResource(id = R.color.grey),
                unfocusedContainerColor = colorResource(id = R.color.grey)
            ), value = message, onValueChange = { newMessage ->
                onMessageChange(newMessage)
            }, keyboardActions = KeyboardActions(onSend = {
                onSendClick()
                keyboardController?.hide()
            }), keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Send
            ), placeholder = {
                Text(
                    text = "Type Here...", color = Color.White.copy(0.7f)
                )
            }, modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .border(
                    width = 1.dp,
                    color = colorResource(id = R.color.grey),
                    shape = RoundedCornerShape(20.dp)
                ), trailingIcon = {
                if (!loading) {
                    if (message.isNotEmpty()) {
                        Icon(
                            modifier = Modifier
                                .size(30.dp)
                                .padding(end = 10.dp)
                                .clickable { onSendClick() },
                            imageVector = Icons.Default.Send,
                            tint = Color.White,
                            contentDescription = stringResource(R.string.send_message),
                        )
                    } else {
                        Icon(
                            modifier = Modifier
                                .size(40.dp)
                                .padding(end = 10.dp)
                                .clickable { onAddImageClick() },
                            painter = painterResource(id = R.drawable.gemini_gallery),
                            tint = Color.White,
                            contentDescription = stringResource(R.string.send_message),
                        )
                    }
                }
            })
        }
    }

}
