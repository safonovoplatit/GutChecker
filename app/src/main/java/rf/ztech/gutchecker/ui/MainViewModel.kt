package rf.ztech.gutchecker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import rf.ztech.gutchecker.data.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val episodeDao = db.episodeDao()
    private val profileDao = db.profileDao()

    val allEpisodes: StateFlow<List<Episode>> = episodeDao.getAllEpisodes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfile> = profileDao.getProfile()
        .map { it ?: UserProfile() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    // 1️⃣ Здоровый стрик (дней без эпизодов)
    val streak: StateFlow<Int> = allEpisodes.map { episodes ->
        calculateHealthyStreak(episodes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // --- Stats for Plaques ---
    val casesToday: StateFlow<Int> = allEpisodes.map { episodes ->
        val todayStart = getStartOfDay()
        episodes.count { it.timestamp >= todayStart }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val casesLast7Days: StateFlow<Int> = allEpisodes.map { episodes ->
        val weekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        episodes.count { it.timestamp >= weekAgo }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val casesLast30Days: StateFlow<Int> = allEpisodes.map { episodes ->
        val monthAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        episodes.count { it.timestamp >= monthAgo }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private fun getStartOfDay(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    // --- AI Chat Logic (Disabled for now) ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("Привет! AI-чат временно недоступен, так как мы работаем над улучшением локальной модели. Вы все еще можете добавлять записи и видеть сводку за день.", false)
    ))
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        val userMsg = ChatMessage(text, true)
        _chatMessages.value = _chatMessages.value + userMsg
        
        _chatMessages.value = _chatMessages.value + ChatMessage("AI-ассистент временно отключен.", false)
    }

    // --- AI Daily Tip (Offline fallback) ---
    private val _dailyTip = MutableStateFlow("Пейте достаточное количество воды в течение дня.")
    val dailyTip: StateFlow<String> = _dailyTip.asStateFlow()

    private val offlineTips = listOf(
        "Пейте достаточное количество воды в течение дня.",
        "Употребляйте больше клетчатки: овощи, фрукты и цельнозерновые продукты.",
        "Старайтесь питаться небольшими порциями, но чаще.",
        "Регулярная физическая активность помогает пищеварению.",
        "Избегайте избыточного потребления кофеина и сахара.",
        "Тщательно пережевывайте пищу — это первый шаг к хорошему пищеварению.",
        "Пробиотики в йогуртах или кефире полезны для микрофлоры кишечника."
    )

    private fun refreshDailyTip() {
        val randomTip = offlineTips.random()
        _dailyTip.value = randomTip
    }

    // --- AI-сводка (Always Rule-based for now) ---
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _aiSummary = MutableStateFlow("Данных за сегодня пока нет.")
    val aiSummary: StateFlow<String> = _aiSummary.asStateFlow()

    init {
        // Автоматически обновляем сводку при изменении эпизодов
        allEpisodes.onEach { episodes ->
            refreshAiSummary(episodes)
        }.launchIn(viewModelScope)
        
        refreshDailyTip()
    }

    fun refreshAiSummary() {
        refreshAiSummary(allEpisodes.value)
    }

    private fun refreshAiSummary(episodes: List<Episode>) {
        val todayStart = getStartOfDay()
        val todayEpisodes = episodes.filter { it.timestamp >= todayStart }

        if (todayEpisodes.isEmpty()) {
            _aiSummary.value = "Сегодня записей не зафиксировано. Продолжайте наблюдение."
            return
        }

        _aiSummary.value = generateRuleBasedSummary(todayEpisodes)
    }

    private fun generateRuleBasedSummary(todayEpisodes: List<Episode>): String {
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
        }
        summary.append("\n\nЭто автоматическая сводка на основе ваших данных.")
        return summary.toString()
    }

    val last7DaysStats: StateFlow<List<Pair<String, Int>>> = allEpisodes.map { episodes ->
        val stats = mutableListOf<Pair<String, Int>>()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

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

    fun addEpisode(consistency: Int, color: String, volume: StoolVolume, hasPain: Boolean, hasBloating: Boolean, hasFever: Boolean, hasNausea: Boolean, note: String, diarrheaLevel: Int) {
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

    private fun calculateHealthyStreak(episodes: List<Episode>): Int {
        if (episodes.isEmpty()) return 0

        val todayStart = getStartOfDay()
        
        // If there were episodes today, the healthy streak is 0
        if (episodes.any { it.timestamp >= todayStart }) return 0

        // Find the most recent episode
        val lastEpisodeTimestamp = episodes.maxOfOrNull { it.timestamp } ?: return 0

        // Calculate full days between today's start and the last episode's day start
        val lastEpisodeCalendar = Calendar.getInstance()
        lastEpisodeCalendar.timeInMillis = lastEpisodeTimestamp
        lastEpisodeCalendar.set(Calendar.HOUR_OF_DAY, 0)
        lastEpisodeCalendar.set(Calendar.MINUTE, 0)
        lastEpisodeCalendar.set(Calendar.SECOND, 0)
        lastEpisodeCalendar.set(Calendar.MILLISECOND, 0)

        val diffMillis = todayStart - lastEpisodeCalendar.timeInMillis
        return (diffMillis / TimeUnit.DAYS.toMillis(1)).toInt()
    }
}
