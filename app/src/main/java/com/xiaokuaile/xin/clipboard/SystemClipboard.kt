package com.xiaokuaile.xin.clipboard

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build

/** 剪贴板里的一项：纯文字，加上来源应用提供的 HTML 格式（保留颜色等）。 */
data class ClipboardText(val text: String, val html: String?)

object SystemClipboard {

    /** 读取当前剪贴板文字。安卓 10+ 只有在前台且窗口有焦点时才能读到。 */
    fun read(context: Context): ClipboardText? {
        val item = firstItem(context) ?: return null
        val text = item.coerceToText(context)?.toString()?.takeIf { it.isNotBlank() } ?: return null
        return ClipboardText(text, item.htmlText?.takeIf { it.isNotBlank() })
    }

    /** 剪贴板里的带格式文字（颜色、粗体等会保留成 span）。 */
    fun readStyled(context: Context): CharSequence? {
        val item = firstItem(context) ?: return null
        return item.coerceToStyledText(context)?.takeIf { it.isNotBlank() }
    }

    /** 剪贴板里的图片地址（比如从相册或浏览器复制的图片）。 */
    fun readImageUri(context: Context): Uri? {
        val cm = context.getSystemService(ClipboardManager::class.java) ?: return null
        val clip = cm.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        val uri = clip.getItemAt(0).uri ?: return null
        val type = context.contentResolver.getType(uri) ?: return null
        return uri.takeIf { type.startsWith("image/") }
    }

    fun write(context: Context, text: String, html: String? = null) {
        val cm = context.getSystemService(ClipboardManager::class.java) ?: return
        val data = if (html != null) ClipData.newHtmlText("Xin", text, html) else ClipData.newPlainText("Xin", text)
        cm.setPrimaryClip(data)
    }

    private fun firstItem(context: Context): ClipData.Item? {
        val cm = context.getSystemService(ClipboardManager::class.java) ?: return null
        val clip = cm.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        if (isSensitive(clip.description)) return null
        return clip.getItemAt(0)
    }

    private fun isSensitive(description: ClipDescription): Boolean {
        val extras = description.extras ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE, false)
        } else {
            extras.getBoolean("android.content.extra.IS_SENSITIVE", false)
        }
    }
}
