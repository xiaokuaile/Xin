package com.xiaokuaile.xin.entry

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.xiaokuaile.xin.R
import com.xiaokuaile.xin.XinApp
import kotlinx.coroutines.launch

/** 处理“分享到 Xin”和文字选择菜单里的“存到 Xin”。 */
class ReceiveActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            else -> null
        }
        if (text.isNullOrBlank()) {
            finish()
            return
        }
        lifecycleScope.launch {
            (application as XinApp).repository.save(text)
            Toast.makeText(this@ReceiveActivity, R.string.saved, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
