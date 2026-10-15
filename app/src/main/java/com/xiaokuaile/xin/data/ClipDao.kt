package com.xiaokuaile.xin.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipDao {
    @Query(
        """
        SELECT * FROM clips
        WHERE (:pinnedOnly = 0 OR pinned = 1)
          AND (:kind IS NULL OR kind = :kind)
          AND (:query = '' OR content LIKE '%' || :query || '%')
        ORDER BY updatedAt DESC
        """
    )
    fun observe(query: String, pinnedOnly: Boolean, kind: String?): Flow<List<Clip>>

    @Query("SELECT * FROM clips WHERE content = :content LIMIT 1")
    suspend fun findByContent(content: String): Clip?

    @Query("SELECT * FROM clips WHERE id = :id")
    suspend fun get(id: Long): Clip?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(clip: Clip): Long

    @Update
    suspend fun update(clip: Clip)

    @Query("DELETE FROM clips WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM clips WHERE pinned = 0")
    suspend fun clearUnpinned()

    @Query("DELETE FROM clips WHERE pinned = 0 AND id NOT IN (SELECT id FROM clips WHERE pinned = 0 ORDER BY updatedAt DESC LIMIT :keep)")
    suspend fun trimUnpinned(keep: Int)
}
