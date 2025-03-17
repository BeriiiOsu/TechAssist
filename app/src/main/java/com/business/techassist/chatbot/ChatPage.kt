package com.business.techassist.chatbot

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.business.techassist.R
import java.util.regex.Pattern


val GeminiDarkBackground = Color(0xFF202124)
val GeminiAccent = Color(0xFF8AB4F8)
val GeminiMessageModel = Color(0xFF3C4043)
val GeminiMessageUser = Color(0xFF8AB4F8)

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun ChatPage(modifier: Modifier = Modifier, viewModel: ChatViewModel) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GeminiDarkBackground)
    ) {
        AppHeader()
        MessageList(
            modifier = Modifier.weight(1f),
            messageList = viewModel.messageList
        )
        MessageInput(
            onMessageSend = {
                viewModel.sendMessage(it)
            }
        )
    }
}

@Composable
fun MessageList(modifier: Modifier = Modifier, messageList: List<MessageModel>) {
    if (messageList.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                modifier = Modifier.size(60.dp),
                painter = painterResource(id = R.drawable.baseline_question_answer_24),
                contentDescription = "Icon",
                tint = GeminiAccent,
            )
            Text(text = "Powered by Gemini", fontSize = 22.sp, color = Color.White)
        }
    } else {
        LazyColumn(
            modifier = modifier,
            reverseLayout = true
        ) {
            items(messageList.reversed()) {
                MessageRow(messageModel = it)
            }
        }
    }
}

@Composable
fun MessageRow(messageModel: MessageModel) {
    val isModel = messageModel.role == "model"

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .align(if (isModel) Alignment.BottomStart else Alignment.BottomEnd)
                    .padding(
                        start = if (isModel) 8.dp else 70.dp,
                        end = if (isModel) 70.dp else 8.dp,
                        top = 8.dp,
                        bottom = 8.dp
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isModel) GeminiMessageModel else GeminiMessageUser)
                    .padding(16.dp)
            ) {
                SelectionContainer {
                    Text(
                        text = formatBoldText(messageModel.message),
                        fontWeight = FontWeight.W500,
                        color = Color.White
                    )
                }
            }
        }
    }
}

fun formatBoldText(text: String): AnnotatedString {
    val pattern = Pattern.compile("\\*\\*(.*?)\\*\\*")
    val matcher = pattern.matcher(text)
    return buildAnnotatedString {
        var lastIndex = 0
        while (matcher.find()) {
            append(text.substring(lastIndex, matcher.start()))
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append(matcher.group(1) ?: "")
            pop()
            lastIndex = matcher.end()
        }
        append(text.substring(lastIndex))
    }
}

@Composable
fun MessageInput(onMessageSend: (String) -> Unit) {
    var message by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .padding(8.dp)
            .background(GeminiDarkBackground)
            .clip(RoundedCornerShape(12.dp))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            modifier = Modifier
                .weight(1f)
                .background(GeminiDarkBackground, RoundedCornerShape(8.dp)),
            value = message,
            onValueChange = { message = it },
            placeholder = { Text(text = "Describe your problem...", color = Color.Gray) },
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White)
        )
        IconButton(onClick = {
            if (message.isNotEmpty()) {
                onMessageSend(message)
                message = ""
            }
        }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = GeminiAccent
            )
        }
    }
}

@Composable
fun AppHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GeminiDarkBackground)
    ) {
        Text(
            modifier = Modifier.padding(16.dp),
            text = "TechAssist Chatbot",
            color = GeminiAccent,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        )
    }
}
