package com.xiaokuaile.xin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.xiaokuaile.xin.XinApp
import com.xiaokuaile.xin.data.Clip
import com.xiaokuaile.xin.data.ClipFilter
import com.xiaokuaile.xin.data.ClipRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val repository: ClipRepository) : ViewModel() {

    val query = MutableStateFlow("")
    val filter = MutableStateFlow(ClipFilter.ALL)

    @OptIn(ExperimentalCoroutinesApi::class)
    val clips: StateFlow<List<Clip>> = combine(query, filter) { q, f -> q to f }
        .flatMapLatest { (q, f) -> repository.observe(q, f) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var lastCaptured: String? = null

    /** 应用回到前台时调用，把剪贴板里的新内容收进历史。 */
    fun captureFromClipboard(text: String?) {
        if (text == null || text == lastCaptured) return
        lastCaptured = text
        viewModelScope.launch { repository.save(text) }
    }

    /** 从 Xin 里复制出去的内容，回到前台时不需要再收录一次。 */
    fun markCopied(text: String) {
        lastCaptured = text
        viewModelScope.launch { repository.save(text) }
    }

    fun add(text: String) = viewModelScope.launch { repository.save(text) }
    fun edit(clip: Clip, text: String) = viewModelScope.launch { repository.edit(clip, text) }
    fun togglePin(clip: Clip) = viewModelScope.launch { repository.togglePin(clip) }
    fun delete(clip: Clip) = viewModelScope.launch { repository.delete(clip) }
    fun restore(clip: Clip) = viewModelScope.launch { repository.restore(clip) }
    fun clearUnpinned() = viewModelScope.launch { repository.clearUnpinned() }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as XinApp
                MainViewModel(app.repository)
            }
        }
    }
}
