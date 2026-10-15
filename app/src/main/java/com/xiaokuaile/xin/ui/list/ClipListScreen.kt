package com.xiaokuaile.xin.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xiaokuaile.xin.R
import com.xiaokuaile.xin.clipboard.SystemClipboard
import com.xiaokuaile.xin.data.Clip
import com.xiaokuaile.xin.data.ClipFilter
import com.xiaokuaile.xin.ui.LaunchAction
import com.xiaokuaile.xin.ui.MainViewModel
import com.xiaokuaile.xin.ui.edit.EditClipDialog
import com.xiaokuaile.xin.ui.poster.PosterActivity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipListScreen(
    viewModel: MainViewModel,
    launchAction: LaunchAction?,
    onLaunchActionHandled: () -> Unit,
) {
    val context = LocalContext.current
    val clips by viewModel.clips.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()

    var searching by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<EditTarget?>(null) }
    var actionsFor by remember { mutableStateOf<Clip?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val searchFocus = remember { FocusRequester() }

    val copiedMsg = stringResource(R.string.copied)
    val deletedMsg = stringResource(R.string.deleted)
    val undoLabel = stringResource(R.string.undo)

    fun copy(clip: Clip) {
        SystemClipboard.write(context, clip.content, clip.html)
        viewModel.markCopied(clip.content)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(copiedMsg)
        }
    }

    fun delete(clip: Clip) {
        viewModel.delete(clip)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            if (snackbar.showSnackbar(deletedMsg, undoLabel) == SnackbarResult.ActionPerformed) {
                viewModel.restore(clip)
            }
        }
    }

    LaunchedEffect(launchAction) {
        when (launchAction) {
            LaunchAction.NEW -> editing = EditTarget.New
            LaunchAction.SEARCH -> searching = true
            null -> return@LaunchedEffect
        }
        onLaunchActionHandled()
    }
    LaunchedEffect(searching) {
        if (searching) searchFocus.requestFocus()
    }

    Scaffold(
        topBar = {
            if (searching) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = {
                            searching = false
                            viewModel.query.value = ""
                        }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                    },
                    title = {
                        TextField(
                            value = query,
                            onValueChange = { viewModel.query.value = it },
                            placeholder = { Text(stringResource(R.string.search_hint)) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchFocus),
                        )
                    },
                    actions = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.query.value = "" }) {
                                Icon(Icons.Filled.Close, stringResource(R.string.clear_search))
                            }
                        }
                    },
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                    actions = {
                        IconButton(onClick = { context.startActivity(PosterActivity.intent(context)) }) {
                            Icon(Icons.Outlined.Image, stringResource(R.string.poster_title))
                        }
                        IconButton(onClick = { searching = true }) {
                            Icon(Icons.Filled.Search, stringResource(R.string.search))
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Filled.MoreVert, stringResource(R.string.more))
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.clear_unpinned)) },
                                    onClick = {
                                        menuOpen = false
                                        confirmClear = true
                                    },
                                )
                            }
                        }
                    },
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = EditTarget.New }) {
                Icon(Icons.Filled.Add, stringResource(R.string.new_clip))
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            FilterRow(selected = filter, onSelect = { viewModel.filter.value = it })
            if (clips.isEmpty()) {
                EmptyState(filter = filter, searching = query.isNotBlank())
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(clips, key = { it.id }) { clip ->
                        ClipRow(
                            clip = clip,
                            highlight = query,
                            onClick = { copy(clip) },
                            onLongClick = { actionsFor = clip },
                            onTogglePin = { viewModel.togglePin(clip) },
                            onSwipeDelete = { delete(clip) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }

    actionsFor?.let { clip ->
        ClipActionsSheet(
            clip = clip,
            onDismiss = { actionsFor = null },
            onCopy = { copy(clip) },
            onTogglePin = { viewModel.togglePin(clip) },
            onEdit = { editing = EditTarget.Existing(clip) },
            onMakeImage = { context.startActivity(PosterActivity.intent(context, clip.id)) },
            onDelete = { delete(clip) },
        )
    }

    editing?.let { target ->
        EditClipDialog(
            initial = (target as? EditTarget.Existing)?.clip?.content.orEmpty(),
            isNew = target is EditTarget.New,
            onDismiss = { editing = null },
            onSave = { text ->
                when (target) {
                    EditTarget.New -> viewModel.add(text)
                    is EditTarget.Existing -> viewModel.edit(target.clip, text)
                }
                editing = null
            },
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.clear_unpinned)) },
            text = { Text(stringResource(R.string.clear_unpinned_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearUnpinned()
                    confirmClear = false
                }) { Text(stringResource(R.string.clear)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

private sealed interface EditTarget {
    data object New : EditTarget
    data class Existing(val clip: Clip) : EditTarget
}

@Composable
private fun FilterRow(selected: ClipFilter, onSelect: (ClipFilter) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        listOf(
            ClipFilter.ALL to R.string.filter_all,
            ClipFilter.PINNED to R.string.filter_pinned,
            ClipFilter.LINKS to R.string.filter_links,
        ).forEach { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(stringResource(label)) },
            )
        }
    }
}

@Composable
private fun EmptyState(filter: ClipFilter, searching: Boolean) {
    val message = when {
        searching -> R.string.empty_search
        filter == ClipFilter.PINNED -> R.string.empty_pinned
        filter == ClipFilter.LINKS -> R.string.empty_links
        else -> R.string.empty_all
    }
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
