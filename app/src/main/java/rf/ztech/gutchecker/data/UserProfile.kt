package rf.ztech.gutchecker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Gender {
    MALE, FEMALE, OTHER, NOT_SPECIFIED
}

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1, // Single profile
    val firstName: String = "",
    val lastName: String = "",
    val height: Int = 0,
    val weight: Float = 0f,
    val age: Int = 0,
    val gender: Gender = Gender.NOT_SPECIFIED,
    val avatarUri: String? = null,
    val isPrivateMode: Boolean = false
)
