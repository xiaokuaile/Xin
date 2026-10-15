package com.xiaokuaile.xin.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateOf
import com.xiaokuaile.xin.clipboard.SystemClipboard
import com.xiaokuaile.xin.ui.list.ClipListScreen
import com.xiaokuaile.xin.ui.theme.XinTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }
    private val pendingAction = mutableStateOf<LaunchAction?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) pendingAction.value = LaunchAction.from(intent)
        setContent {
            XinTheme {
                ClipListScreen(
                    viewModel = viewModel,
                    launchAction = pendingAction.value,
                    onLaunchActionHandled = { pendingAction.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingAction.value = LaunchAction.from(intent)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // 安卓 10+ 只有拿到焦点之后才读得到剪贴板
        if (hasFocus) viewModel.captureFromClipboard(SystemClipboard.read(this))
    }
}

enum class LaunchAction {
    NEW, SEARCH;

    companion object {
        const val ACTION_NEW = "com.xiaokuaile.xin.action.NEW"
        const val ACTION_SEARCH = "com.xiaokuaile.xin.action.SEARCH"

        fun from(intent: Intent?): LaunchAction? = when {
            intent == null -> null
            intent.action == ACTION_NEW || intent.data?.host == "new" -> NEW
            intent.action == ACTION_SEARCH || intent.data?.host == "search" -> SEARCH
            else -> null
        }
    }
}
