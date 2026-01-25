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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import coil.compose.AsyncImage
import rf.ztech.gutchecker.data.*
import rf.ztech.gutchecker.ui.MainViewModel
import rf.ztech.gutchecker.ui.theme.GutCheckerTheme
import java.text.SimpleDateFormat
import java.util.*

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Главная", Icons.Default.Home)
    object History : Screen("history", "История", Icons.Default.List)
    object Profile : Screen("profile", "Профиль", Icons.Default.Person)
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
                val items = listOf(Screen.Dashboard, Screen.History, Screen.Profile)

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
            composable(Screen.History.route) { HistoryScreen(viewModel) }
            composable(Screen.Profile.route) { ProfileScreen(viewModel) }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val stats by viewModel.last7DaysStats.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val dailyTip by viewModel.dailyTip.let { remember { mutableStateOf(it) } }
    val aiSummary by viewModel.dailySummary.collectAsState()
    
    val isPrivate = userProfile.isPrivateMode
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(32.dp)) }
        
        item {
            Text("🏠 Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        // 1️⃣ Стрик дней с поносом
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isPrivate) "***" else streak.toString(),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "🔥 Стрик дней с поносом\n(подряд дней с записями)",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        // 2️⃣ Совет дня
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🥗 Совет дня", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(dailyTip, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        // 3️⃣ AI-сводка за сегодня
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
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
            Card(modifier = Modifier.fillMaxWidth()) {
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
fun HistoryScreen(viewModel: MainViewModel) {
    val episodes by viewModel.allEpisodes.collectAsState()
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Spacer(modifier = Modifier.height(32.dp))
        Text("История записей", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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
fun ProfileScreen(viewModel: MainViewModel) {
    val profile by viewModel.userProfile.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val isPrivate = profile.isPrivateMode

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
        Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer).clickable { launcher.launch("image/*") }, contentAlignment = Alignment.Center) {
            if (profile.avatarUri != null) {
                AsyncImage(model = profile.avatarUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(60.dp))
            }
        }
        Text("Нажмите, чтобы изменить фото", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileField("Имя", profile.firstName) { viewModel.updateProfile(profile.copy(firstName = it)) }
                ProfileField("Фамилия", profile.lastName) { viewModel.updateProfile(profile.copy(lastName = it)) }
                ProfileField("Рост (см)", profile.height.toString()) { viewModel.updateProfile(profile.copy(height = it.toIntOrNull() ?: 0)) }
                ProfileField("Вес (кг)", profile.weight.toString()) { viewModel.updateProfile(profile.copy(weight = it.toFloatOrNull() ?: 0f)) }
                ProfileField("Возраст", profile.age.toString()) { viewModel.updateProfile(profile.copy(age = it.toIntOrNull() ?: 0)) }
                
                Text("Пол")
                Row {
                    Gender.entries.filter { it != Gender.NOT_SPECIFIED }.forEach { g ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { viewModel.updateProfile(profile.copy(gender = g)) }) {
                            RadioButton(selected = profile.gender == g, onClick = { viewModel.updateProfile(profile.copy(gender = g)) })
                            Text(g.name)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
}

@Composable
fun ProfileField(label: String, value: String, onValueChange: (String) -> Unit) {
    TextField(value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
}
