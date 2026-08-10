package ir.yadavar.birthdays

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import ir.yadavar.birthdays.data.AppDatabase
import ir.yadavar.birthdays.data.Birthday
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ReminderScheduler.schedule(this)
        if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        enableEdgeToEdge()
        val database = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "birthdays.db").build()
        setContent { BirthdayApp(database) }
    }
}

private val Rose = Color(0xFF8C3E67)
private val Ink = Color(0xFF312731)
private val Cream = Color(0xFFFFF8F8)

@Composable
fun BirthdayApp(database: AppDatabase) {
    val people by database.birthdayDao().observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    var addOpen by remember { mutableStateOf(false) }
    var editingBirthday by remember { mutableStateOf<Birthday?>(null) }
    var selectedMonth by remember { mutableStateOf<Int?>(null) }
    val today = remember { JalaliDate.fromGregorian(Calendar.getInstance()) }
    val filteredPeople = remember(people, selectedMonth) { selectedMonth?.let { month -> people.filter { it.month == month } } ?: people }
    val scheme = lightColorScheme(primary = Rose, secondary = Color(0xFFB85D87), background = Cream, surface = Color.White, onBackground = Ink)

    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = scheme, typography = Typography(bodyLarge = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.SansSerif), titleLarge = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.SansSerif))) {
            Scaffold(containerColor = Cream, floatingActionButton = {
                ExtendedFloatingActionButton(onClick = { addOpen = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("ثبت تولد") }, containerColor = Rose, contentColor = Color.White)
            }) { padding ->
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { WelcomeHeader(today, people.size) }
                    item {
                        Text("تفکیک بر اساس ماه", style = MaterialTheme.typography.titleLarge, color = Ink, modifier = Modifier.padding(top = 12.dp))
                        MonthFilter(selectedMonth = selectedMonth, onSelect = { selectedMonth = it })
                    }
                    item {
                        val title = selectedMonth?.let { "تولدهای ${persianMonths[it - 1]}" } ?: "همهٔ تولدها"
                        Text(title, style = MaterialTheme.typography.titleLarge, color = Ink, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                    }
                    if (filteredPeople.isEmpty()) item { EmptyState(selectedMonth) }
                    items(filteredPeople, key = { it.id }) { person ->
                        BirthdayCard(person = person, onEdit = { editingBirthday = person }, onDelete = { scope.launch { database.birthdayDao().delete(person) } })
                    }
                }
            }
            if (addOpen || editingBirthday != null) {
                BirthdayFormDialog(
                    birthday = editingBirthday,
                    onDismiss = { addOpen = false; editingBirthday = null },
                    onSave = { birthday ->
                        scope.launch {
                            if (editingBirthday == null) database.birthdayDao().insert(birthday) else database.birthdayDao().update(birthday)
                            addOpen = false
                            editingBirthday = null
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun WelcomeHeader(today: JalaliDate, count: Int) {
    Surface(shape = RoundedCornerShape(28.dp), color = Rose, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(18.dp), color = Color.White.copy(alpha = .18f)) { Icon(Icons.Default.Cake, null, tint = Color.White, modifier = Modifier.padding(14.dp).size(30.dp)) }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("یادآور تولد", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Text("امروز ${today.display()} • $count تولد ثبت‌شده", color = Color.White.copy(alpha = .86f), modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun MonthFilter(selectedMonth: Int?, onSelect: (Int?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
        item { FilterChip(selected = selectedMonth == null, onClick = { onSelect(null) }, label = { Text("همه") }) }
        items(persianMonths.size) { index ->
            val month = index + 1
            FilterChip(selected = selectedMonth == month, onClick = { onSelect(month) }, label = { Text(persianMonths[index]) })
        }
    }
}

@Composable
private fun EmptyState(selectedMonth: Int?) {
    Surface(shape = RoundedCornerShape(24.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 38.dp, horizontal = 20.dp)) {
            Icon(Icons.Default.Cake, null, tint = Color(0xFFE1A0BC), modifier = Modifier.size(44.dp))
            Text(if (selectedMonth == null) "هنوز تولدی ثبت نشده" else "برای ${persianMonths[selectedMonth - 1]} تولدی ثبت نشده", style = MaterialTheme.typography.titleMedium, color = Ink, modifier = Modifier.padding(top = 12.dp))
            Text("با دکمهٔ «ثبت تولد» شروع کنید.", color = Color.Gray, modifier = Modifier.padding(top = 5.dp))
        }
    }
}

@Composable
private fun BirthdayCard(person: Birthday, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFFFE9F1)) { Icon(Icons.Default.Person, null, tint = Rose, modifier = Modifier.padding(11.dp).size(24.dp)) }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(person.name, style = MaterialTheme.typography.titleMedium, color = Ink)
                Text("${person.day} ${persianMonths[person.month - 1]}${person.year?.let { " $it" } ?: ""}", color = Color(0xFF7D6874), modifier = Modifier.padding(top = 3.dp))
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "ویرایش", tint = Rose) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, "حذف", tint = Color(0xFFB54B58)) }
        }
    }
}

@Composable
private fun BirthdayFormDialog(birthday: Birthday?, onDismiss: () -> Unit, onSave: (Birthday) -> Unit) {
    var name by remember(birthday?.id) { mutableStateOf(birthday?.name.orEmpty()) }
    var dayText by remember(birthday?.id) { mutableStateOf(birthday?.day?.toString().orEmpty()) }
    var yearText by remember(birthday?.id) { mutableStateOf(birthday?.year?.toString().orEmpty()) }
    var month by remember(birthday?.id) { mutableIntStateOf(birthday?.month ?: 1) }
    var expanded by remember { mutableStateOf(false) }
    val day = dayText.toIntOrNull()
    val valid = name.isNotBlank() && day != null && day in 1..if (month <= 6) 31 else 30
    val isEditing = birthday != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "ویرایش تولد" else "ثبت تولد جدید", color = Ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("نام شخص") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = dayText, onValueChange = { dayText = it.filter(Char::isDigit).take(2) }, label = { Text("روز") }, singleLine = true, modifier = Modifier.weight(.42f))
                    Box(Modifier.weight(.58f)) {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(5.dp)) { Text(persianMonths[month - 1], modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { persianMonths.forEachIndexed { index, label -> DropdownMenuItem(text = { Text(label) }, onClick = { month = index + 1; expanded = false }) } }
                    }
                }
                OutlinedTextField(value = yearText, onValueChange = { yearText = it.filter(Char::isDigit).take(4) }, label = { Text("سال تولد (اختیاری)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("یادآوری یک روز قبل، حدود ساعت ۹ صبح ارسال می‌شود.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onSave(Birthday(id = birthday?.id ?: 0, name = name.trim(), day = day!!, month = month, year = yearText.toIntOrNull())) }) { Text(if (isEditing) "ذخیرهٔ تغییرات" else "ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
