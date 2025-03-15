package com.business.techassist.chatbot

import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.business.techassist.chatbot.ui.theme.TechAssistTheme

class ChatAssistFragment : Fragment() {

    private lateinit var chatViewModel: ChatViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatViewModel = ViewModelProvider(this)[ChatViewModel::class.java]
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View {
        return ComposeView(requireContext()).apply {
            setContent {
                TechAssistTheme {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        ChatPage(modifier = Modifier.padding(innerPadding), chatViewModel)
                    }
                }
            }
        }
    }
}
