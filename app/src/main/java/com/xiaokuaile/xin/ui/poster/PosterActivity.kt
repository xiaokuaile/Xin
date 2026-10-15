package com.xiaokuaile.xin.ui.poster

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.xiaokuaile.xin.ui.theme.XinTheme

/** 文字转图片：图文混排编辑，导出成一张长图。 */
class PosterActivity : ComponentActivity() {
    private val viewModel: PosterViewModel by viewModels { PosterViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            XinTheme {
                PosterScreen(viewModel = viewModel, onBack = { finish() })
            }
        }
    }

    companion object {
        const val EXTRA_CLIP_ID = "clip_id"

        fun intent(context: Context, clipId: Long? = null): Intent =
            Intent(context, PosterActivity::class.java).apply {
                if (clipId != null) putExtra(EXTRA_CLIP_ID, clipId)
            }
    }
}
