package com.xiaokuaile.xin.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ClipKind {
    TEXT, LINK, PHONE, EMAIL;

    companion object
}

@Entity(tableName = "clips", indices = [Index(value = ["content"], unique = true)])
data class Clip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    /** 复制时带的 HTML 格式（颜色、粗体等），没有就是 null。 */
    val html: String? = null,
    val kind: ClipKind = ClipKind.detect(content),
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
)

private val linkRegex = Regex("""^(https?://|www\.)\S+$""", RegexOption.IGNORE_CASE)
private val emailRegex = Regex("""^[\w.+-]+@[\w-]+(\.[\w-]+)+$""")
private val phoneRegex = Regex("""^\+?[\d\s-]{6,20}$""")

fun ClipKind.Companion.detect(text: String): ClipKind {
    val t = text.trim()
    return when {
        linkRegex.matches(t) -> ClipKind.LINK
        emailRegex.matches(t) -> ClipKind.EMAIL
        phoneRegex.matches(t) && t.count { it.isDigit() } >= 6 -> ClipKind.PHONE
        else -> ClipKind.TEXT
    }
}
