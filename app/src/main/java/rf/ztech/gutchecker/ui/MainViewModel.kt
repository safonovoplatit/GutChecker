package rf.ztech.gutchecker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import rf.ztech.gutchecker.data.*
import java.util.*
import java.util.concurrent.TimeUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val episodeDao = db.episodeDao()
    private val profileDao = db.profileDao()

    val allEpisodes: StateFlow<List<Episode>> = episodeDao.getAllEpisodes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfile> = profileDao.getProfile()
        .map { it ?: UserProfile() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    // 2️⃣ Список советов по питанию (не медицинские)
    private val healthTips = listOf(
        "Пейте достаточно воды в течение дня для поддержания гидратации.",
        "Старайтесь есть медленно и тщательно пережевывать пищу.",
        "Регулярное питание в одно и то же время помогает настроить пищеварение.",
        "Попробуйте добавить в рацион больше продуктов с мягкой клетчаткой.",
        "Ведите дневник питания, чтобы заметить связь между продуктами и симптомами.",
        "Ограничьте употребление газированных напитков во время приема пищи.",
        "Легкая прогулка после еды может улучшить моторику кишечника."
    )

    val dailyTip: String = healthTips[Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % healthTips.size]

    // 1️⃣ Стрик (дни С эпизодами)
    val streak: StateFlow<Int> = allEpisodes.map { episodes ->
        calculateActiveStreak(episodes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // 3️⃣ AI-сводка за текущий день (rule-based эвристика)
    val dailySummary: StateFlow<String> = allEpisodes.map { episodes ->
        generateDailySummary(episodes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Данных за сегодня пока нет.")

    val last7DaysStats: StateFlow<List<Pair<String, Int>>> = allEpisodes.map { episodes ->
        val stats = mutableListOf<Pair<String, Int>>()
        val dayFormat = java.text.SimpleDateFormat("EEE", Locale.getDefault())

        for (i in 6 downTo 0) {
            val dayCalendar = Calendar.getInstance()
            dayCalendar.add(Calendar.DAY_OF_YEAR, -i)
            dayCalendar.set(Calendar.HOUR_OF_DAY, 0)
            dayCalendar.set(Calendar.MINUTE, 0)
            dayCalendar.set(Calendar.SECOND, 0)
            dayCalendar.set(Calendar.MILLISECOND, 0)
            
            val startOfDay = dayCalendar.timeInMillis
            val endOfDay = startOfDay + TimeUnit.DAYS.toMillis(1)
            
            val count = episodes.count { it.timestamp in startOfDay until endOfDay }
            stats.add(dayFormat.format(dayCalendar.time) to count)
        }
        stats
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addEpisode(
        consistency: Int,
        color: String,
        volume: StoolVolume,
        hasPain: Boolean,
        hasBloating: Boolean,
        hasFever: Boolean,
        hasNausea: Boolean,
        note: String,
        diarrheaLevel: Int
    ) {
        viewModelScope.launch {
            episodeDao.insertEpisode(Episode(
                timestamp = System.currentTimeMillis(),
                consistency = consistency,
                color = color,
                volume = volume,
                hasPain = hasPain,
                hasBloating = hasBloating,
                hasFever = hasFever,
                hasNausea = hasNausea,
                note = note,
                diarrheaLevel = diarrheaLevel
            ))
        }
    }

    fun deleteEpisode(episode: Episode) {
        viewModelScope.launch {
            episodeDao.deleteEpisode(episode)
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            episodeDao.deleteAllEpisodes()
        }
    }

    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch {
            profileDao.updateProfile(profile)
        }
    }

    fun setPrivateMode(enabled: Boolean) {
        viewModelScope.launch {
            val current = userProfile.value
            profileDao.updateProfile(current.copy(isPrivateMode = enabled))
        }
    }

    private fun calculateActiveStreak(episodes: List<Episode>): Int {
        if (episodes.isEmpty()) return 0
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        var checkDateStart = calendar.timeInMillis
        var streakCount = 0

        // Проверяем, были ли эпизоды сегодня
        val episodesToday = episodes.any { it.timestamp >= checkDateStart }
        if (!episodesToday) return 0 // Стрик прерывается, если сегодня записей нет

        while (true) {
            val hasEpisodesOnDay = episodes.any { 
                it.timestamp >= checkDateStart && it.timestamp < checkDateStart + TimeUnit.DAYS.toMillis(1) 
            }
            
            if (hasEpisodesOnDay) {
                streakCount++
                checkDateStart -= TimeUnit.DAYS.toMillis(1)
            } else {
                break
            }
            if (streakCount > 365) break 
        }
        return streakCount
    }

    private fun generateDailySummary(episodes: List<Episode>): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis
        
        val todayEpisodes = episodes.filter { it.timestamp >= todayStart }
        if (todayEpisodes.isEmpty()) return "Сегодня записей не зафиксировано. Продолжайте наблюдение."

        val count = todayEpisodes.size
        val avgLevel = todayEpisodes.map { it.diarrheaLevel }.average()
        val symptomCount = todayEpisodes.count { it.hasPain || it.hasBloating || it.hasFever || it.hasNausea }
        
        val levelText = when {
            avgLevel <= 3 -> "низкий"
            avgLevel <= 7 -> "умеренный"
            else -> "высокий"
        }

        val summary = StringBuilder()
        summary.append("Сегодня зафиксировано $count ${if (count == 1) "эпизод" else "эпизода"}. ")
        summary.append("Средний уровень дискомфорта — $levelText (${String.format("%.1f", avgLevel)}). ")
        
        if (symptomCount > 0) {
            summary.append("Сопутствующие симптомы отмечены в $symptomCount ${if (symptomCount == 1) "записи" else "записях"}. ")
        } else {
            summary.append("Дополнительных симптомов не зафиксировано. ")
        }

        // Эвристика по времени
        val morningCount = todayEpisodes.count { 
            val h = Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.HOUR_OF_DAY)
            h in 6..11
        }
        if (morningCount > count / 2) {
            summary.append("Большинство эпизодов произошло в первой половине дня.")
        }

        return summary.toString()
    }
}
