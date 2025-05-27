package com.business.techassist.chatbot

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.business.techassist.chatbot.ui.theme.TechAssistTheme
import com.business.techassist.subscription.FeatureLockManager
import com.business.techassist.subscription.SubscriptionActivity

class ChatAssistFragment : Fragment() {

    private lateinit var chatViewModel: ChatViewModel
    private val hasAccess = mutableStateOf(false)
    private val TAG = "ChatAssistFragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatViewModel = ViewModelProvider(this)[ChatViewModel::class.java]
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View {
        // Create ComposeView which will be our fragment's view
        val composeView = ComposeView(requireContext())
        
        // Initialize with empty content - will be updated after access check
        composeView.setContent {
            TechAssistTheme {
                // Empty initial content
            }
        }
        
        // Check feature access before loading UI
        context?.let { ctx ->
            Log.d(TAG, "Checking chatbot access permission")
            FeatureLockManager.checkFeatureAccessAsync(
                ctx,
                FeatureLockManager.FEATURE_CHATBOT,
                object : FeatureLockManager.FeatureAccessCallback {
                    override fun onResult(hasAccessResult: Boolean, message: String) {
                        Log.d(TAG, "Access check result: $hasAccessResult, message: $message")
                        if (hasAccessResult) {
                            // User has access, update the UI on main thread
                            activity?.runOnUiThread {
                                hasAccess.value = true
                                composeView.setContent {
                                    TechAssistTheme {
                                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                                            ChatPage(modifier = Modifier.padding(innerPadding), chatViewModel)
                                        }
                                    }
                                }
                            }
                        } else {
                            // User doesn't have access, show upgrade dialog on main thread
                            activity?.runOnUiThread {
                                activity?.let { activity ->
                                    FeatureLockManager.showUpgradeDialog(
                                        activity,
                                        FeatureLockManager.FEATURE_CHATBOT,
                                        message
                                    )
                                    
                                    // Navigate to subscription page and finish this activity
                                    val intent = Intent(activity, SubscriptionActivity::class.java)
                                    startActivity(intent)
                                    activity.onBackPressed() // Go back from the fragment
                                }
                            }
                        }
                    }
                }
            )
        } ?: run {
            // Context is null, can't check access - safer to deny access
            Log.e(TAG, "Fragment context is null, cannot check access permissions")
            activity?.onBackPressed() // Go back from the fragment
        }
        
        return composeView
    }
    
    override fun onResume() {
        super.onResume()
        
        // Re-check access on resume in case the user upgraded their plan
        if (!hasAccess.value) {
            context?.let { ctx ->
                FeatureLockManager.checkFeatureAccessAsync(
                    ctx,
                    FeatureLockManager.FEATURE_CHATBOT
                ) { hasAccessResult, message ->
                    if (!hasAccessResult) {
                        // Still no access, go back
                        activity?.runOnUiThread {
                            activity?.onBackPressed()
                        }
                    } else {
                        // Access gained, update state
                        hasAccess.value = true
                    }
                }
            }
        }
    }
}
