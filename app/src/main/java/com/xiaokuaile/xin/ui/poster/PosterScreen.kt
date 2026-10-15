package com.xiaokuaile.xin.ui.poster

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.text.InputType
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.Gravity
import android.widget.EditText
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.xiaokuaile.xin.R
import com.xiaokuaile.xin.clipboard.SystemClipboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosterScreen(viewModel: PosterViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val editors = remember { mutableMapOf<Long, EditText>() }
    var focusedKey by remember { mutableStateOf<Long?>(null) }
    var paletteOpen by remember { mutableStateOf(false) }
    var styleOpen by remember { mutableStateOf(false) }

    val theme = viewModel.theme
    val textSize = viewModel.textSize

    fun message(res: Int) {
        val text = context.getString(res)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(text)
        }
    }

    fun focusedEditor(): EditText? = focusedKey?.let(editors::get)?.takeIf { it.hasFocus() }

    fun insertImages(uris: List<android.net.Uri>) {
        val editor = focusedEditor()
        viewModel.insertImages(uris, editor?.let { focusedKey }, editor?.selectionStart?.takeIf { it >= 0 })
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(9)) { uris ->
        insertImages(uris)
    }

    fun paste() {
        SystemClipboard.readImageUri(context)?.let { clipUri ->
            val copied = PosterImages.copyToAppStorage(context, clipUri)
            if (copied != null) insertImages(listOf(copied)) else message(R.string.poster_image_failed)
            return
        }
        val styled = SystemClipboard.readStyled(context)
        if (styled == null) {
            message(R.string.clipboard_empty)
            return
        }
        val editor = focusedEditor()
        if (editor != null) {
            val start = minOf(editor.selectionStart, editor.selectionEnd).coerceAtLeast(0)
            val end = maxOf(editor.selectionStart, editor.selectionEnd).coerceAtLeast(0)
            editor.text.replace(start, end, styled)
        } else {
            viewModel.appendText(styled)
        }
    }

    fun export(share: Boolean) {
        if (viewModel.isEmpty) {
            message(R.string.poster_empty)
            return
        }
        scope.launch {
            viewModel.busy = true
            val result = runCatching {
                val (bitmap, hasPhoto) = viewModel.render(context)
                try {
                    withContext(Dispatchers.IO) {
                        if (share) {
                            PosterSaver.shareIntent(context, bitmap, hasPhoto)
                        } else {
                            PosterSaver.saveToGallery(context, bitmap, hasPhoto)
                        }
                    }
                } finally {
                    bitmap.recycle()
                }
            }
            viewModel.busy = false
            val value = result.getOrNull()
            when {
                value is android.content.Intent -> context.startActivity(value)
                value != null -> message(R.string.poster_saved)
                else -> message(R.string.poster_save_failed)
            }
        }
    }

    val storagePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) export(share = false) else message(R.string.poster_need_permission)
    }

    fun save() {
        val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            export(share = false)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                title = { Text(stringResource(R.string.poster_title)) },
                actions = {
                    if (viewModel.busy) {
                        CircularProgressIndicator(Modifier.padding(12.dp).size(24.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { export(share = true) }) {
                            Icon(Icons.Filled.Share, stringResource(R.string.share))
                        }
                        IconButton(onClick = ::save) {
                            Icon(Icons.Filled.Download, stringResource(R.string.poster_save))
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    horizontalArrangement = Arrangement.SpaceAround,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(vertical = 4.dp),
                ) {
                    ToolButton(Icons.Filled.ContentPaste, R.string.poster_paste, ::paste)
                    ToolButton(Icons.Filled.AddPhotoAlternate, R.string.poster_add_image) {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    ToolButton(Icons.Filled.FormatColorText, R.string.poster_text_color) {
                        if (focusedEditor()?.let { it.selectionStart != it.selectionEnd } == true) {
                            paletteOpen = true
                        } else {
                            message(R.string.poster_select_first)
                        }
                    }
                    ToolButton(Icons.Filled.Palette, R.string.poster_style) { styleOpen = true }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Surface(
                color = Color(theme.background),
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    viewModel.blocks.forEachIndexed { index, block ->
                        key(block.key) {
                            when (block) {
                                is TextBlock -> TextBlockEditor(
                                    block = block,
                                    theme = theme,
                                    textSize = textSize,
                                    hint = if (index == 0) stringResource(R.string.poster_hint) else null,
                                    onFocus = { focusedKey = block.key },
                                    register = { editors[block.key] = it },
                                    unregister = { editors.remove(block.key) },
                                )
                                is ImageBlock -> ImageBlockView(block, onRemove = { viewModel.removeImage(block) })
                            }
                        }
                    }
                    if (viewModel.showFooter) {
                        Text(
                            text = "· Xin",
                            color = Color(theme.secondary),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.align(Alignment.End),
                        )
                    }
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }

    if (paletteOpen) {
        ModalBottomSheet(onDismissRequest = { paletteOpen = false }) {
            ColorPalette(
                onPick = { color ->
                    focusedEditor()?.let { applyColor(it, color) }
                    paletteOpen = false
                },
            )
        }
    }

    if (styleOpen) {
        ModalBottomSheet(onDismissRequest = { styleOpen = false }) {
            StyleSheet(viewModel)
        }
    }
}

@Composable
private fun ToolButton(icon: ImageVector, label: Int, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null)
            Text(stringResource(label), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun TextBlockEditor(
    block: TextBlock,
    theme: PosterTheme,
    textSize: PosterTextSize,
    hint: String?,
    onFocus: () -> Unit,
    register: (EditText) -> Unit,
    unregister: () -> Unit,
) {
    AndroidView(
        factory = { ctx ->
            EditText(ctx).apply {
                background = null
                setPadding(0, 0, 0, 0)
                gravity = Gravity.TOP or Gravity.START
                inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                isSingleLine = false
                setLineSpacing(0f, 1.35f)
                // 系统的长按“粘贴”也会保留颜色等格式
                setText(block.text, TextView.BufferType.EDITABLE)
                block.text = text
                setOnFocusChangeListener { _, hasFocus -> if (hasFocus) onFocus() }
                register(this)
            }
        },
        update = { editor ->
            editor.setTextColor(theme.text)
            editor.setHintTextColor(theme.secondary)
            editor.setLinkTextColor(theme.link)
            editor.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize.editorSp)
            editor.hint = hint
        },
        onRelease = { unregister() },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ImageBlockView(block: ImageBlock, onRemove: () -> Unit) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, block.uri) {
        value = withContext(Dispatchers.IO) {
            PosterImages.decode(context, block.uri, PosterRenderer.WIDTH)?.asImageBitmap()
        }
    }
    Box(Modifier.fillMaxWidth()) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(image.width.toFloat() / image.height)
                    .clip(RoundedCornerShape(6.dp)),
            )
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(32.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape),
        ) {
            Icon(Icons.Filled.Close, stringResource(R.string.delete), tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPalette(onPick: (Int?) -> Unit) {
    Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
        Text(stringResource(R.string.poster_text_color), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            PosterPalette.forEach { color ->
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(color))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .clickable { onPick(color) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = { onPick(null) }) { Text(stringResource(R.string.poster_color_reset)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StyleSheet(viewModel: PosterViewModel) {
    Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.poster_background), style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PosterTheme.entries.forEach { option ->
                FilterChip(
                    selected = viewModel.theme == option,
                    onClick = { viewModel.theme = option },
                    label = { Text(stringResource(option.label)) },
                    leadingIcon = {
                        Box(
                            Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(option.background))
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                        )
                    },
                )
            }
        }
        Text(stringResource(R.string.poster_font_size), style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PosterTextSize.entries.forEach { option ->
                FilterChip(
                    selected = viewModel.textSize == option,
                    onClick = { viewModel.textSize = option },
                    label = { Text(stringResource(option.label)) },
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.poster_footer), modifier = Modifier.weight(1f))
            Switch(checked = viewModel.showFooter, onCheckedChange = { viewModel.showFooter = it })
        }
    }
}

/** 给选中的文字上色；[color] 为 null 时去掉选中范围内的颜色，恢复默认。 */
private fun applyColor(editor: EditText, color: Int?) {
    val text = editor.text
    val start = minOf(editor.selectionStart, editor.selectionEnd)
    val end = maxOf(editor.selectionStart, editor.selectionEnd)
    if (start < 0 || start == end) return
    // 先把选区里原有的颜色剪掉，选区外的部分保留
    text.getSpans(start, end, ForegroundColorSpan::class.java).forEach { span ->
        val spanStart = text.getSpanStart(span)
        val spanEnd = text.getSpanEnd(span)
        text.removeSpan(span)
        if (spanStart < start) {
            text.setSpan(ForegroundColorSpan(span.foregroundColor), spanStart, start, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        if (spanEnd > end) {
            text.setSpan(ForegroundColorSpan(span.foregroundColor), end, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
    if (color != null) {
        text.setSpan(ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}
