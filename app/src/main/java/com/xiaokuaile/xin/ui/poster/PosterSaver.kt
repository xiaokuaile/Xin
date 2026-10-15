package com.xiaokuaile.xin.ui.poster

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

object PosterSaver {

    /** 有照片就存 JPEG（体积小），纯文字存 PNG（文字边缘更清晰）。 */
    private fun format(hasPhoto: Boolean) =
        if (hasPhoto) Bitmap.CompressFormat.JPEG to "jpg" else Bitmap.CompressFormat.PNG to "png"

    private fun fileName(ext: String) = "Xin_${System.currentTimeMillis()}.$ext"

    /** 保存到相册的 Pictures/Xin。安卓 9 及以下需要先拿到存储权限。 */
    fun saveToGallery(context: Context, bitmap: Bitmap, hasPhoto: Boolean): Uri? {
        val (format, ext) = format(hasPhoto)
        val mime = if (ext == "png") "image/png" else "image/jpeg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName(ext))
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Xin")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        val ok = runCatching {
            resolver.openOutputStream(uri)?.use { bitmap.compress(format, 92, it) } ?: false
        }.getOrDefault(false)
        if (!ok) {
            resolver.delete(uri, null, null)
            return null
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        }
        return uri
    }

    /** 写到缓存目录，返回可以分享给其他应用的地址。 */
    fun shareIntent(context: Context, bitmap: Bitmap, hasPhoto: Boolean): Intent {
        val (format, ext) = format(hasPhoto)
        val dir = File(context.cacheDir, "posters").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, fileName(ext))
        file.outputStream().use { bitmap.compress(format, 92, it) }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType(if (ext == "png") "image/png" else "image/jpeg")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return Intent.createChooser(send, null)
    }
}
