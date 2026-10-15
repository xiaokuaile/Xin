package com.xiaokuaile.xin.entry

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.xiaokuaile.xin.R
import com.xiaokuaile.xin.XinApp
import com.xiaokuaile.xin.clipboard.SystemClipboard
import kotlinx.coroutines.launch

/**
 * 没有界面的“一键收录”页。安卓 10 起后台应用读不到剪贴板，
 * 所以快捷设置磁贴先把这个透明页拉到前台，等窗口拿到焦点再读。
 */
class CaptureActivity : ComponentActivity() {
    private var handled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handled = savedInstanceState?.getBoolean(KEY_HANDLED) ?: false
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || handled) return
        handled = true
        val text = SystemClipboard.read(this)
        if (text == null) {
            Toast.makeText(this, R.string.clipboard_empty, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        lifecycleScope.launch {
            val isNew = (application as XinApp).repository.save(text)
            Toast.makeText(
                this@CaptureActivity,
                if (isNew) R.string.saved else R.string.saved_existing,
                Toast.LENGTH_SHORT,
            ).show()
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_HANDLED, handled)
    }

    private companion object {
        const val KEY_HANDLED = "handled"
    }
}
