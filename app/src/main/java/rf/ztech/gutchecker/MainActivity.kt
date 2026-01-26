package rf.ztech.gutchecker

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import coil.compose.AsyncImage
import rf.ztech.gutchecker.data.*
import rf.ztech.gutchecker.ui.ChatMessage
import rf.ztech.gutchecker.ui.MainViewModel
import rf.ztech.gutchecker.ui.theme.GutCheckerTheme
import java.text.SimpleDateFormat
import java.util.*

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Главная", Icons.Default.Home)
    object Chat : Screen("chat", "AI Чат", Icons.Default.Chat)
    object History : Screen("history", "История", Icons.Default.List)
    object Profile : Screen("profile", "Профиль", Icons.Default.Person)
    object Calendar : Screen("calendar", "Календарь", Icons.Default.CalendarMonth)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GutCheckerTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val items = listOf(Screen.Dashboard, Screen.Chat, Screen.History, Screen.Profile)

                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = Screen.Dashboard.route, Modifier.padding(innerPadding)) {
            composable(Screen.Dashboard.route) { DashboardScreen(viewModel) }
            composable(Screen.Chat.route) { ChatScreen(viewModel) }
            composable(Screen.History.route) { HistoryScreen(viewModel, navController) }
            composable(Screen.Profile.route) { ProfileScreen(viewModel) }
            composable(Screen.Calendar.route) { CalendarScreen(viewModel, navController) }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val stats by viewModel.last7DaysStats.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val aiSummary by viewModel.aiSummary.collectAsState()
    val dailyTip by viewModel.dailyTip.collectAsState()
    
    val casesToday by viewModel.casesToday.collectAsState()
    val cases7Days by viewModel.casesLast7Days.collectAsState()
    val cases30Days by viewModel.casesLast30Days.collectAsState()
    
    val isPrivate = userProfile.isPrivateMode
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(32.dp)) }
        
        item {
            Text("GutKeeper", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        // 1️⃣ Healthy Streak Element
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFFFF3E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Здоровый стрик",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A237E)
                        )
                        Text(
                            text = "ДНЕЙ БЕЗ ЭПИЗОДОВ",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF90A4AE),
                            letterSpacing = 0.5.sp
                        )
                    }

                    Text(
                        text = if (isPrivate) "***" else streak.toString(),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100)
                    )
                }
            }
        }

        // 2️⃣ Organic Stats Plaques
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatPlaque(
                    modifier = Modifier.weight(1f),
                    label = "Сегодня",
                    value = casesToday.toString(),
                    isPrivate = isPrivate,
                    containerColor = Color(0xFFE8F5E9),
                    contentColor = Color(0xFF2E7D32)
                )
                StatPlaque(
                    modifier = Modifier.weight(1f),
                    label = "7 дней",
                    value = cases7Days.toString(),
                    isPrivate = isPrivate,
                    containerColor = Color(0xFFE3F2FD),
                    contentColor = Color(0xFF1565C0)
                )
                StatPlaque(
                    modifier = Modifier.weight(1f),
                    label = "30 дней",
                    value = cases30Days.toString(),
                    isPrivate = isPrivate,
                    containerColor = Color(0xFFFFF3E0),
                    contentColor = Color(0xFFE65100)
                )
            }
        }

        // 3️⃣ AI Daily Tip
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🥗 Совет дня от AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(dailyTip, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        // 4️⃣ AI Summary
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🤖 AI-сводка за сегодня", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isPrivate) "Данные скрыты в приватном режиме." else aiSummary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Сводка носит информационный характер и не является медицинской рекомендацией",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📊 График (7 дней)", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (isPrivate) {
                        Box(Modifier.fillMaxWidth().height(150.dp).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                            Text("***", fontSize = 48.sp)
                        }
                    } else {
                        SimpleChart(stats)
                    }
                }
            }
        }

        item {
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Добавить эпизод")
            }
        }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    if (showAddDialog) {
        AddEpisodeDialog(
            onDismiss = { showAddDialog = false },
            onSave = { consistency, color, volume, pain, bloating, fever, nausea, note, level ->
                viewModel.addEpisode(consistency, color, volume, pain, bloating, fever, nausea, note, level)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun StatPlaque(
    modifier: Modifier,
    label: String,
    value: String,
    isPrivate: Boolean,
    containerColor: Color = Color.White,
    contentColor: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 16.dp, horizontal = 8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isPrivate) "***" else value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = contentColor
            )
        }
    }
}

@Composable
fun ChatScreen(viewModel: MainViewModel) {
    val messages by viewModel.chatMessages.collectAsState()
    val isLoading by viewModel.isChatLoading.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Spacer(modifier = Modifier.height(32.dp))
        Text("AI Ассистент", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message)
            }
            if (isLoading) {
                item {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Задайте вопрос...") },
                maxLines = 3
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                },
                enabled = !isLoading
            ) {
                Icon(Icons.Default.Send, contentDescription = "Отправить")
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val color = if (message.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (message.isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
    
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(
            color = color,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun SimpleChart(data: List<Pair<String, Int>>) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurface
    
    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp).padding(8.dp)) {
        if (data.isEmpty()) return@Canvas
        
        val maxVal = (data.maxByOrNull { it.second }?.second ?: 0).coerceAtLeast(5)
        val width = size.width
        val height = size.height
        val barWidth = width / (data.size * 1.5f)
        val spacing = width / data.size

        data.forEachIndexed { index, pair ->
            val barHeight = (pair.second.toFloat() / maxVal) * (height - 40f)
            val x = index * spacing + spacing / 4f
            
            drawRect(
                color = primaryColor,
                topLeft = Offset(x, height - 30f - barHeight),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
            )
            
            drawContext.canvas.nativeCanvas.drawText(
                pair.first,
                x + barWidth / 2,
                height,
                android.graphics.Paint().apply {
                    color = labelColor.hashCode()
                    textSize = 30f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )

            drawContext.canvas.nativeCanvas.drawText(
                pair.second.toString(),
                x + barWidth / 2,
                height - 35f - barHeight,
                android.graphics.Paint().apply {
                    color = labelColor.hashCode()
                    textSize = 25f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEpisodeDialog(onDismiss: () -> Unit, onSave: (Int, String, StoolVolume, Boolean, Boolean, Boolean, Boolean, String, Int) -> Unit) {
    var consistency by remember { mutableFloatStateOf(4f) }
    var color by remember { mutableStateOf("Коричневый") }
    var volume by remember { mutableStateOf(StoolVolume.MEDIUM) }
    var hasPain by remember { mutableStateOf(false) }
    var hasBloating by remember { mutableStateOf(false) }
    var hasFever by remember { mutableStateOf(false) }
    var hasNausea by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var diarrheaLevel by remember { mutableFloatStateOf(1f) }

    val colors = listOf("Коричневый", "Жёлтый", "Зелёный", "Чёрный", "Красный")
    var colorExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onSave(consistency.toInt(), color, volume, hasPain, hasBloating, hasFever, hasNausea, note, diarrheaLevel.toInt())
            }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
        title = { Text("Новая запись") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Консистенция (Бристольская шкала): ${consistency.toInt()}")
                Slider(value = consistency, onValueChange = { consistency = it }, valueRange = 1f..7f, steps = 5)
                
                Text("Цвет")
                ExposedDropdownMenuBox(expanded = colorExpanded, onExpandedChange = { colorExpanded = it }) {
                    TextField(value = color, onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = colorExpanded) }, modifier = Modifier.menuAnchor())
                    ExposedDropdownMenu(expanded = colorExpanded, onDismissRequest = { colorExpanded = false }) {
                        colors.forEach { selectionOption ->
                            DropdownMenuItem(text = { Text(selectionOption) }, onClick = { color = selectionOption; colorExpanded = false })
                        }
                    }
                }

                Text("Объём")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StoolVolume.entries.forEach { v ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { volume = v }) {
                            RadioButton(selected = volume == v, onClick = { volume = v })
                            Text(v.name, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Text("Симптомы")
                Column {
                    SymptomCheck("Боль", hasPain) { hasPain = it }
                    SymptomCheck("Вздутие", hasBloating) { hasBloating = it }
                    SymptomCheck("Температура", hasFever) { hasFever = it }
                    SymptomCheck("Тошнота", hasNausea) { hasNausea = it }
                }

                TextField(value = note, onValueChange = { note = it }, label = { Text("Заметка (опц.)") })

                Text("Уровень поноса: ${diarrheaLevel.toInt()}")
                Slider(value = diarrheaLevel, onValueChange = { diarrheaLevel = it }, valueRange = 1f..10f, steps = 8)
            }
        }
    )
}

@Composable
fun SymptomCheck(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label)
    }
}

@Composable
fun HistoryScreen(viewModel: MainViewModel, navController: NavController) {
    val episodes by viewModel.allEpisodes.collectAsState()
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Spacer(modifier = Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("История записей", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = { navController.navigate(Screen.Calendar.route) }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Календарь", tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        if (episodes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("История пуста")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(episodes) { episode ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1.0f)) {
                                Text(dateFormat.format(Date(episode.timestamp)), fontWeight = FontWeight.Bold)
                                Text("Бристоль: ${episode.consistency}, Цвет: ${episode.color}")
                                Text("Уровень: ${episode.diarrheaLevel}/10")
                            }
                            IconButton(onClick = { viewModel.deleteEpisode(episode) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarScreen(viewModel: MainViewModel, navController: NavController) {
    val episodes by viewModel.allEpisodes.collectAsState()
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    
    val episodeCounts = remember(episodes) {
        val counts = mutableMapOf<String, Int>()
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        episodes.forEach {
            val key = df.format(Date(it.timestamp))
            counts[key] = counts.getOrDefault(key, 0) + 1
        }
        counts
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Spacer(modifier = Modifier.height(32.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Календарь", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        CalendarView(
            month = currentMonth,
            episodeCounts = episodeCounts,
            onMonthChange = { currentMonth = it }
        )
    }
}

@Composable
fun CalendarView(month: Calendar, episodeCounts: Map<String, Int>, onMonthChange: (Calendar) -> Unit) {
    val monthName = SimpleDateFormat("LLLL yyyy", Locale.getDefault()).format(month.time).replaceFirstChar { it.uppercase() }
    
    val daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = (month.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }.get(Calendar.DAY_OF_WEEK)
    val offset = (firstDayOfWeek + 5) % 7 // Adjust for Monday start

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onMonthChange((month.clone() as Calendar).apply { add(Calendar.MONTH, -1) }) }) {
                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Prev")
            }
            Text(monthName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = { onMonthChange((month.clone() as Calendar).apply { add(Calendar.MONTH, 1) }) }) {
                Icon(Icons.Default.ArrowForwardIos, contentDescription = "Next")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { day ->
                Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        val totalCells = ((daysInMonth + offset + 6) / 7) * 7
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.height(300.dp)) {
            items((0 until totalCells).toList()) { index ->
                val day = index - offset + 1
                if (day in 1..daysInMonth) {
                    val dateStr = df.format(Calendar.getInstance().apply {
                        timeInMillis = month.timeInMillis
                        set(Calendar.DAY_OF_MONTH, day)
                    }.time)
                    val count = episodeCounts[dateStr] ?: 0
                    
                    Box(modifier = Modifier.aspectRatio(1f).padding(2.dp).clip(RoundedCornerShape(8.dp)).background(if(count > 0) MaterialTheme.colorScheme.primaryContainer else Color.Transparent), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(day.toString(), fontWeight = FontWeight.SemiBold)
                            if (count > 0) {
                                Text(count.toString(), fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.aspectRatio(1f))
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(viewModel: MainViewModel) {
    val profile by viewModel.userProfile.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var editingName by remember { mutableStateOf("") }
    var editingValue by remember { mutableStateOf("") }
    var editingType by remember { mutableStateOf("text") } // text, number, gender
    var onSaveAction by remember { mutableStateOf<(String) -> Unit>({}) }

    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            viewModel.updateProfile(profile.copy(avatarUri = uri.toString()))
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(32.dp))
        Text("Профиль", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Avatar
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .clickable { launcher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (profile.avatarUri != null) {
                AsyncImage(
                    model = profile.avatarUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Text("Нажмите, чтобы изменить фото", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(24.dp))

        // Name Plaque
        ProfileItemCard(
            label = "Имя и Фамилия",
            value = "${profile.firstName} ${profile.lastName}".trim().ifEmpty { "Не указано" },
            onEdit = {
                editingName = "Имя и Фамилия"
                editingValue = "${profile.firstName} ${profile.lastName}".trim()
                editingType = "text"
                onSaveAction = {
                    val parts = it.split(" ", limit = 2)
                    val first = parts.getOrNull(0) ?: ""
                    val last = parts.getOrNull(1) ?: ""
                    viewModel.updateProfile(profile.copy(firstName = first, lastName = last))
                }
                showEditDialog = true
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Stats Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileStatItem(Modifier.weight(1f), "Рост", "${profile.height} см", Icons.Default.Height) {
                editingName = "Рост (см)"
                editingValue = profile.height.toString()
                editingType = "number"
                onSaveAction = { viewModel.updateProfile(profile.copy(height = it.toIntOrNull() ?: 0)) }
                showEditDialog = true
            }
            ProfileStatItem(Modifier.weight(1f), "Вес", "${profile.weight} кг", Icons.Default.MonitorWeight) {
                editingName = "Вес (кг)"
                editingValue = profile.weight.toString()
                editingType = "number"
                onSaveAction = { viewModel.updateProfile(profile.copy(weight = it.toFloatOrNull() ?: 0f)) }
                showEditDialog = true
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileStatItem(Modifier.weight(1f), "Возраст", "${profile.age}", Icons.Default.CalendarToday) {
                editingName = "Возраст"
                editingValue = profile.age.toString()
                editingType = "number"
                onSaveAction = { viewModel.updateProfile(profile.copy(age = it.toIntOrNull() ?: 0)) }
                showEditDialog = true
            }
            ProfileStatItem(Modifier.weight(1f), "Пол", if(profile.gender == Gender.MALE) "Мужской" else if(profile.gender == Gender.FEMALE) "Женский" else "Другой", Icons.Default.Wc) {
                editingName = "Пол"
                editingType = "gender"
                showEditDialog = true
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Приватный режим", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Switch(checked = profile.isPrivateMode, onCheckedChange = { viewModel.setPrivateMode(it) })
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Информация", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("GutCheck работает полностью автономно.\nВаши данные хранятся только на этом устройстве и никогда не передаются в облако.")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { viewModel.deleteAllData() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Удалить все записи")
        }
        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            confirmButton = {
                if (editingType != "gender") {
                    TextButton(onClick = { onSaveAction(editingValue); showEditDialog = false }) { Text("ОК") }
                }
            },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("Отмена") } },
            title = { Text(editingName) },
            text = {
                if (editingType == "gender") {
                    Column {
                        Gender.entries.filter { it != Gender.NOT_SPECIFIED }.forEach { g ->
                            val label = if(g == Gender.MALE) "Мужской" else if(g == Gender.FEMALE) "Женский" else "Другой"
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { 
                                viewModel.updateProfile(profile.copy(gender = g))
                                showEditDialog = false 
                            }.padding(8.dp)) {
                                RadioButton(selected = profile.gender == g, onClick = null)
                                Text(label, Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                } else {
                    TextField(value = editingValue, onValueChange = { editingValue = it }, singleLine = true)
                }
            }
        )
    }
}

@Composable
fun ProfileItemCard(label: String, value: String, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun ProfileStatItem(modifier: Modifier, label: String, value: String, icon: ImageVector, onEdit: () -> Unit) {
    Card(
        modifier = modifier.clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProfileField(label: String, value: String, onValueChange: (String) -> Unit) {
    TextField(value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
}
