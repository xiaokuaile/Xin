package com.xiaokuaile.xin.ui.poster

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import android.text.format.DateFormat
import android.text.style.AbsoluteSizeSpan
import android.text.style.AlignmentSpan
import android.text.style.BackgroundColorSpan
import android.text.style.BulletSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.QuoteSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import android.view.inputmethod.BaseInputConnection
import kotlin.math.roundToInt

/**
 * 把编辑框里的文字和图片画成一张长图。文字用 StaticLayout 排版，
 * 粘贴进来的颜色、背景色、粗斜体、下划线、字号等 span 原样保留。
 */
object PosterRenderer {
    const val WIDTH = 1080
    private const val PADDING = 72
    private const val GAP = 36
    private const val MAX_HEIGHT = 16_000
    private const val CONTENT_WIDTH = WIDTH - PADDING * 2

    fun render(
        context: Context,
        blocks: List<PosterBlock>,
        theme: PosterTheme,
        size: PosterTextSize,
        footer: Boolean,
    ): Bitmap {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.text
            linkColor = theme.link
            textSize = size.renderPx
            // 让 HTML 里的 dip 字号（AbsoluteSizeSpan）和编辑时的比例一致
            density = size.renderPx / size.editorSp
        }

        val parts = blocks.mapNotNull { block ->
            when (block) {
                is TextBlock -> cleanText(block.text)?.let { Part.Text(layout(it, paint)) }
                is ImageBlock -> PosterImages.decode(context, block.uri, CONTENT_WIDTH)?.let { Part.Image(it) }
            }
        }

        val footerLayout = if (footer) {
            val footPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.secondary
                textSize = size.renderPx * 0.62f
            }
            val date = DateFormat.getDateFormat(context).format(System.currentTimeMillis())
            val label = "$date · Xin"
            StaticLayout.Builder.obtain(label, 0, label.length, footPaint, CONTENT_WIDTH)
                .setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
                .build()
        } else {
            null
        }

        val contentHeight = parts.sumOf { it.height() } + GAP * (parts.size - 1).coerceAtLeast(0)
        val footerHeight = footerLayout?.let { it.height + GAP * 2 } ?: 0
        val fullHeight = (PADDING * 2 + contentHeight + footerHeight).coerceAtLeast(PADDING * 2 + 1)

        // 特别长的内容整体缩小，避免生成几百 MB 的图片
        val scale = if (fullHeight > MAX_HEIGHT) MAX_HEIGHT.toFloat() / fullHeight else 1f
        val bitmap = Bitmap.createBitmap(
            (WIDTH * scale).roundToInt(),
            (fullHeight * scale).roundToInt(),
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)
        canvas.scale(scale, scale)
        canvas.drawColor(theme.background)

        var y = PADDING.toFloat()
        val imagePaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        parts.forEachIndexed { index, part ->
            if (index > 0) y += GAP
            when (part) {
                is Part.Text -> {
                    canvas.save()
                    canvas.translate(PADDING.toFloat(), y)
                    part.layout.draw(canvas)
                    canvas.restore()
                }
                is Part.Image -> {
                    val dst = Rect(PADDING, y.toInt(), PADDING + CONTENT_WIDTH, y.toInt() + part.height())
                    canvas.drawBitmap(part.bitmap, null, dst, imagePaint)
                }
            }
            y += part.height()
        }
        footerLayout?.let {
            canvas.save()
            canvas.translate(PADDING.toFloat(), y + GAP * 2)
            it.draw(canvas)
            canvas.restore()
        }
        parts.filterIsInstance<Part.Image>().forEach { it.bitmap.recycle() }
        return bitmap
    }

    private sealed interface Part {
        fun height(): Int

        class Text(val layout: StaticLayout) : Part {
            override fun height() = layout.height
        }

        class Image(val bitmap: Bitmap) : Part {
            override fun height() = bitmap.height * CONTENT_WIDTH / bitmap.width
        }
    }

    private fun layout(text: CharSequence, paint: TextPaint): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, CONTENT_WIDTH)
            .setLineSpacing(0f, 1.35f)
            .setIncludePad(true)
            .build()

    /** 复制一份文字，只留下影响外观的格式；去掉输入法的拼写、组字标记和图片占位符。 */
    private fun cleanText(source: CharSequence): CharSequence? {
        val copy = SpannableStringBuilder(source)
        BaseInputConnection.removeComposingSpans(copy)
        copy.getSpans(0, copy.length, Any::class.java).forEach { span ->
            if (!isVisualStyle(span)) copy.removeSpan(span)
        }
        var i = copy.length - 1
        while (i >= 0) {
            if (copy[i] == '￼') copy.delete(i, i + 1)
            i--
        }
        // 去掉首尾空行，但保留 span
        while (copy.isNotEmpty() && copy[copy.length - 1].isWhitespace()) copy.delete(copy.length - 1, copy.length)
        while (copy.isNotEmpty() && copy[0] == '\n') copy.delete(0, 1)
        return copy.takeIf { it.isNotEmpty() }
    }

    private fun isVisualStyle(span: Any): Boolean = when (span) {
        is ForegroundColorSpan, is BackgroundColorSpan, is StyleSpan, is UnderlineSpan,
        is StrikethroughSpan, is RelativeSizeSpan, is AbsoluteSizeSpan, is TypefaceSpan,
        is SuperscriptSpan, is SubscriptSpan, is QuoteSpan, is BulletSpan, is URLSpan,
        is AlignmentSpan, is LeadingMarginSpan -> true
        else -> false
    }
}
