package com.xiaokuaile.xin.data

import kotlinx.coroutines.flow.Flow

class ClipRepository(private val dao: ClipDao) {

    fun observe(query: String, filter: ClipFilter): Flow<List<Clip>> = dao.observe(
        query = query.trim(),
        pinnedOnly = filter == ClipFilter.PINNED,
        kind = if (filter == ClipFilter.LINKS) ClipKind.LINK.name else null,
    )

    /** 保存一条内容；已存在则移到最前。返回 true 表示是新内容。 */
    suspend fun save(raw: String): Boolean {
        val content = raw.trimEnd()
        if (content.isBlank()) return false
        val now = System.currentTimeMillis()
        val existing = dao.findByContent(content)
        if (existing != null) {
            dao.update(existing.copy(updatedAt = now))
            return false
        }
        dao.insert(Clip(content = content, createdAt = now))
        dao.trimUnpinned(MAX_UNPINNED)
        return true
    }

    suspend fun edit(clip: Clip, newContent: String) {
        val content = newContent.trimEnd()
        if (content.isBlank() || content == clip.content) return
        val clash = dao.findByContent(content)
        if (clash != null && clash.id != clip.id) {
            // 改成了另一条已有内容：合并到那一条，保留收藏状态
            dao.update(clash.copy(pinned = clash.pinned || clip.pinned, updatedAt = System.currentTimeMillis()))
            dao.delete(clip.id)
        } else {
            dao.update(clip.copy(content = content, kind = ClipKind.detect(content), updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun togglePin(clip: Clip) = dao.update(clip.copy(pinned = !clip.pinned))

    suspend fun delete(clip: Clip) = dao.delete(clip.id)

    suspend fun restore(clip: Clip) {
        dao.insert(clip)
    }

    suspend fun clearUnpinned() = dao.clearUnpinned()

    companion object {
        const val MAX_UNPINNED = 500
    }
}

enum class ClipFilter { ALL, PINNED, LINKS }
