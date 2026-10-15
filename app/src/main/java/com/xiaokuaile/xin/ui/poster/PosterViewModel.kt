package com.xiaokuaile.xin.ui.poster

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.text.Editable
import android.text.Html
import android.text.SpannableStringBuilder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.xiaokuaile.xin.XinApp
import com.xiaokuaile.xin.data.ClipRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PosterViewModel(
    private val repository: ClipRepository,
    savedState: SavedStateHandle,
) : ViewModel() {

    private var nextKey = 0L
    private fun key() = nextKey++

    val blocks = mutableStateListOf<PosterBlock>(TextBlock(key(), newEditable("")))
    var theme by mutableStateOf(PosterTheme.PAPER)
    var textSize by mutableStateOf(PosterTextSize.MEDIUM)
    var showFooter by mutableStateOf(true)
    var busy by mutableStateOf(false)

    init {
        val clipId = savedState.get<Long>(PosterActivity.EXTRA_CLIP_ID)
        if (clipId != null && clipId > 0) {
            viewModelScope.launch {
                val clip = repository.get(clipId) ?: return@launch
                val styled: CharSequence = clip.html
                    ?.let { Html.fromHtml(it, Html.FROM_HTML_MODE_COMPACT).trimEnd() }
                    ?: clip.content
                blocks[0] = TextBlock(key(), newEditable(styled))
            }
        }
    }

    /**
     * 在 [atKey] 这段文字的 [cursor] 位置插入图片：把这段文字一分为二，图片放中间。
     * 没有指定位置就加到最后。
     */
    fun insertImages(uris: List<Uri>, atKey: Long?, cursor: Int?) {
        if (uris.isEmpty()) return
        val images = uris.map { ImageBlock(key(), it) }
        val index = blocks.indexOfFirst { it.key == atKey }
        val target = blocks.getOrNull(index) as? TextBlock
        if (target == null || cursor == null) {
            val last = blocks.lastOrNull()
            if (last is TextBlock && last.text.isBlank()) {
                blocks.addAll(blocks.size - 1, images)
            } else {
                blocks.addAll(images)
                blocks.add(TextBlock(key(), newEditable("")))
            }
            return
        }
        val text = target.text
        val at = cursor.coerceIn(0, text.length)
        val before = text.subSequence(0, at).trimEnd('\n')
        val after = text.subSequence(at, text.length).trimStart('\n')
        val replacement = buildList {
            if (before.isNotEmpty() || index == 0) add(TextBlock(key(), newEditable(before)))
            addAll(images)
            add(TextBlock(key(), newEditable(after)))
        }
        blocks.removeAt(index)
        blocks.addAll(index, replacement)
    }

    /** 删掉一张图片；上下两段文字接回一段。 */
    fun removeImage(block: ImageBlock) {
        val index = blocks.indexOf(block)
        if (index < 0) return
        blocks.removeAt(index)
        val prev = blocks.getOrNull(index - 1) as? TextBlock
        val next = blocks.getOrNull(index) as? TextBlock
        if (prev != null && next != null) {
            val merged = SpannableStringBuilder(prev.text)
            if (merged.isNotEmpty() && next.text.isNotEmpty()) merged.append('\n')
            merged.append(next.text)
            blocks[index - 1] = TextBlock(key(), newEditable(merged))
            blocks.removeAt(index)
        }
        if (blocks.none { it is TextBlock }) blocks.add(TextBlock(key(), newEditable("")))
    }

    /** 没有聚焦的编辑框时，粘贴的文字接到最后一段。 */
    fun appendText(styled: CharSequence) {
        val last = blocks.lastOrNull() as? TextBlock
        if (last != null) {
            if (last.text.isNotEmpty()) last.text.append('\n')
            last.text.append(styled)
        } else {
            blocks.add(TextBlock(key(), newEditable(styled)))
        }
    }

    val isEmpty: Boolean
        get() = blocks.all { it is TextBlock && it.text.isBlank() }

    /** 在主线程复制一份内容，然后在后台线程画图。 */
    suspend fun render(context: Context): Pair<Bitmap, Boolean> {
        val snapshot = blocks.map { block ->
            when (block) {
                is TextBlock -> TextBlock(block.key, SpannableStringBuilder(block.text))
                is ImageBlock -> block
            }
        }
        val theme = theme
        val size = textSize
        val footer = showFooter
        val bitmap = withContext(Dispatchers.Default) {
            PosterRenderer.render(context.applicationContext, snapshot, theme, size, footer)
        }
        return bitmap to snapshot.any { it is ImageBlock }
    }

    companion object {
        fun newEditable(text: CharSequence): Editable = Editable.Factory.getInstance().newEditable(text)

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as XinApp
                PosterViewModel(app.repository, createSavedStateHandle())
            }
        }
    }
}
