package com.business.techassist.chatbot
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.business.techassist.R
import com.business.techassist.chatbot.ui.theme.ColorModelMessage
import com.business.techassist.chatbot.ui.theme.ColorUserMessage
import com.business.techassist.chatbot.ui.theme.Purple80

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun ChatPage(modifier: Modifier = Modifier, viewModel: ChatViewModel) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
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
fun MessageList(modifier: Modifier = Modifier,messageList : List<MessageModel>) {
    if(messageList.isEmpty()){
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                modifier = Modifier.size(60.dp),
                painter = painterResource(id = R.drawable.baseline_question_answer_24),
                contentDescription = "Icon",
                tint = Purple80,
            )
            Text(text = "Ask me anything", fontSize = 22.sp, color = Color.Black)
        }
    }else{
        LazyColumn(
            modifier = modifier,
            reverseLayout = true
        ) {
            items(messageList.reversed()){
                MessageRow(messageModel = it)
            }
        }
    }
}

@Composable
fun MessageRow(messageModel: MessageModel) {
    val isModel = messageModel.role=="model"

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
                    .clip(RoundedCornerShape(48f))
                    .background(if (isModel) ColorModelMessage else ColorUserMessage)
                    .padding(16.dp)
            ) {

                SelectionContainer {
                    Text(
                        text = messageModel.message,
                        fontWeight = FontWeight.W500,
                        color = Color.White
                    )
                }
            }
        }
    }
}


@Composable
fun MessageInput(onMessageSend : (String)-> Unit) {

    var message by remember {
        mutableStateOf("")
    }

    Row(
        modifier = Modifier.padding(8.dp)
            .padding(8.dp)
            .background(Color.White)
            .clip(RoundedCornerShape(12.dp))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            modifier = Modifier.weight(1f)
                .background(Color.LightGray, RoundedCornerShape(8.dp)),
            value = message,
            onValueChange = {
                message = it
            },
            placeholder = { Text(text = "Type your message...", color = Color.Gray) }, // Placeholder text
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black)
        )
        IconButton(onClick = {
            if(message.isNotEmpty()){
                onMessageSend(message)
                message = ""
            }

        }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = Color.Black
            )
        }
    }
}

@Composable
fun AppHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        Text(
            modifier = Modifier.padding(16.dp),
            text = "Chatbot",
            color = Color.Black,
            fontSize = 22.sp
        )
    }
}