package rf.ztech.gutchecker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class StoolVolume {
    SMALL, MEDIUM, LARGE
}

@Entity(tableName = "episodes")
data class Episode(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val consistency: Int, // 1-7 (Бристольская шкала)
    val color: String,
    val volume: StoolVolume,
    val hasPain: Boolean = false,
    val hasBloating: Boolean = false,
    val hasFever: Boolean = false,
    val hasNausea: Boolean = false,
    val note: String = "",
    val diarrheaLevel: Int // 1-10
)
