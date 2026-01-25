package rf.ztech.gutchecker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeDao {
    @Query("SELECT * FROM episodes ORDER BY timestamp DESC")
    fun getAllEpisodes(): Flow<List<Episode>>

    @Insert
    suspend fun insertEpisode(episode: Episode)

    @Delete
    suspend fun deleteEpisode(episode: Episode)

    @Update
    suspend fun updateEpisode(episode: Episode)

    @Query("DELETE FROM episodes")
    suspend fun deleteAllEpisodes()

    @Query("SELECT * FROM episodes WHERE timestamp >= :startOfDay")
    suspend fun getEpisodesForDay(startOfDay: Long): List<Episode>
}
