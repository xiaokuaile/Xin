package com.xiaokuaile.xin.ui.poster

import android.net.Uri
import android.text.Editable
import androidx.annotation.StringRes
import com.xiaokuaile.xin.R

/** 长图里的一块内容：一段（可能带颜色的）文字，或一张图片。 */
sealed interface PosterBlock {
    val key: Long
}

/**
 * [text] 和编辑框共用同一个 Editable：编辑框改了，这里就是最新的，
 * 粘贴进来的颜色、粗体等格式以 span 的形式保存在里面。
 */
class TextBlock(override val key: Long, var text: Editable) : PosterBlock

class ImageBlock(override val key: Long, val uri: Uri) : PosterBlock

enum class PosterTheme(
    @StringRes val label: Int,
    val background: Int,
    val text: Int,
    val secondary: Int,
    val link: Int,
) {
    PAPER(R.string.poster_theme_paper, 0xFFFFFFFF.toInt(), 0xFF222222.toInt(), 0xFF9AA19E.toInt(), 0xFF13A387.toInt()),
    CREAM(R.string.poster_theme_cream, 0xFFF6F0E3.toInt(), 0xFF3A3226.toInt(), 0xFFA39883.toInt(), 0xFF8C5A2B.toInt()),
    NIGHT(R.string.poster_theme_night, 0xFF1A1D1C.toInt(), 0xFFE8ECEA.toInt(), 0xFF6F7875.toInt(), 0xFF4FD6B6.toInt()),
}

/** 编辑时的字号（sp）和导出图片时的字号（像素，图片宽 1080）。 */
enum class PosterTextSize(@StringRes val label: Int, val editorSp: Float, val renderPx: Float) {
    SMALL(R.string.poster_size_small, 15f, 40f),
    MEDIUM(R.string.poster_size_medium, 17f, 46f),
    LARGE(R.string.poster_size_large, 20f, 54f),
}

/** 调色板：常用的几种文字颜色。 */
val PosterPalette = listOf(
    0xFF222222, 0xFFE53935, 0xFFFB8C00, 0xFFF9A825, 0xFF43A047,
    0xFF13A387, 0xFF1E88E5, 0xFF5E35B1, 0xFFD81B60, 0xFF8D6E63,
).map { it.toInt() }
