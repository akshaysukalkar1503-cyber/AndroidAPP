package com.oosmitra.ai

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val green = androidx.compose.ui.graphics.Color(0xFF176B3A)
private val paleGreen = androidx.compose.ui.graphics.Color(0xFFE7F4EA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OosMitraApp(applicationContext) }
    }
}

private enum class Screen { HOME, FARM, WATER, EXPENSES, NOTES }

@Composable
private fun OosMitraApp(context: Context) {
    val prefs = remember { context.getSharedPreferences("oosmitra_local", Context.MODE_PRIVATE) }
    var screen by remember { mutableStateOf(Screen.HOME) }
    var refresh by remember { mutableIntStateOf(0) }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = green,
            secondary = androidx.compose.ui.graphics.Color(0xFFB87913),
            background = androidx.compose.ui.graphics.Color(0xFFF7F8F5),
            surface = androidx.compose.ui.graphics.Color.White
        )
    ) {
        Scaffold(
            topBar = {
                Surface(color = green) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp)) {
                        Text("🌱 ऊस मित्र AI", style = MaterialTheme.typography.headlineSmall,
                            color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
                        Text("विदर्भातील शेतकऱ्यांसाठी मराठी सहाय्यक · ऑफलाइन",
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color.White)
                    }
                }
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(selected = screen == Screen.HOME, onClick = { screen = Screen.HOME },
                        icon = { Text("⌂") }, label = { Text("मुख्य") })
                    NavigationBarItem(selected = screen == Screen.FARM, onClick = { screen = Screen.FARM },
                        icon = { Text("🌾") }, label = { Text("माझे शेत") })
                    NavigationBarItem(selected = screen == Screen.WATER, onClick = { screen = Screen.WATER },
                        icon = { Text("💧") }, label = { Text("पाणी") })
                    NavigationBarItem(selected = screen == Screen.EXPENSES, onClick = { screen = Screen.EXPENSES },
                        icon = { Text("₹") }, label = { Text("खर्च") })
                    NavigationBarItem(selected = screen == Screen.NOTES, onClick = { screen = Screen.NOTES },
                        icon = { Text("☷") }, label = { Text("नोंदी") })
                }
            }
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                when (screen) {
                    Screen.HOME -> HomeScreen(
                        prefs = prefs,
                        onFarm = { screen = Screen.FARM },
                        onWater = { screen = Screen.WATER },
                        onExpenses = { screen = Screen.EXPENSES },
                        onNotes = { screen = Screen.NOTES }
                    )
                    Screen.FARM -> FarmScreen(prefs) { refresh++ }
                    Screen.WATER -> WaterScreen()
                    Screen.EXPENSES -> ExpenseScreen()
                    Screen.NOTES -> NotesScreen(prefs) { refresh++ }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    prefs: android.content.SharedPreferences,
    onFarm: () -> Unit, onWater: () -> Unit, onExpenses: () -> Unit, onNotes: () -> Unit
) {
    val village = prefs.getString("village", "") ?: ""
    val district = prefs.getString("district", "यवतमाळ") ?: "यवतमाळ"
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = paleGreen)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (village.isBlank()) "नमस्कार शेतकरी मित्रा!" else "नमस्कार! $village",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = green)
                Text("तुमची शेतीची माहिती या मोबाईलमध्येच जतन होते.")
                Text("जिल्हा: $district  •  इंटरनेट आवश्यक नाही", style = MaterialTheme.typography.bodySmall)
            }
        }
        Text("आजचे पर्याय", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        FeatureCard("💧", "पाणी व्यवस्थापन", "जमिनीतील ओलावा व पावसावर आधारित प्राथमिक सल्ला", onWater)
        FeatureCard("🌾", "माझे शेत", "गाव, क्षेत्र, जमीन, पाण्याचा स्रोत आणि पीक अवस्था जतन करा", onFarm)
        FeatureCard("₹", "खर्च व नफा", "एकरी खर्च, अपेक्षित उत्पादन आणि उत्पन्नाचा अंदाज", onExpenses)
        FeatureCard("📝", "शेत नोंदवही", "शेतातील कामे, निरीक्षणे आणि पुढील कामाची नोंद", onNotes)
        Card {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("महत्त्वाची सूचना", fontWeight = FontWeight.Bold)
                Text("हा पहिला ऑफलाइन नमुना आहे. तो थेट हवामान डेटा किंवा प्रशिक्षित रोग-ओळख AI वापरत नाही. कृषी निर्णयासाठी स्थानिक कृषी अधिकारी/कृषी विद्यापीठाचा सल्ला घ्या.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun FeatureCard(icon: String, title: String, detail: String, onClick: () -> Unit) {
    Card(onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(icon, style = MaterialTheme.typography.headlineMedium)
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(detail, style = MaterialTheme.typography.bodySmall)
            }
            Text("›", style = MaterialTheme.typography.headlineMedium, color = green)
        }
    }
}

@Composable
private fun FarmScreen(prefs: android.content.SharedPreferences, onSaved: () -> Unit) {
    var village by remember { mutableStateOf(prefs.getString("village", "") ?: "") }
    var district by remember { mutableStateOf(prefs.getString("district", "यवतमाळ") ?: "यवतमाळ") }
    var area by remember { mutableStateOf(prefs.getString("area", "1") ?: "1") }
    var soil by remember { mutableStateOf(prefs.getString("soil", "काळी जमीन") ?: "काळी जमीन") }
    var water by remember { mutableStateOf(prefs.getString("water", "विहीर / बोअरवेल") ?: "विहीर / बोअरवेल") }
    var stage by remember { mutableStateOf(prefs.getString("stage", "वाढीची अवस्था") ?: "वाढीची अवस्था") }
    var saved by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("माझ्या शेताची माहिती", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        OutlinedTextField(village, { village = it; saved = false }, label = { Text("गावाचे नाव") }, modifier = Modifier.fillMaxWidth())
        DropdownField("जिल्हा", district, listOf("यवतमाळ","अमरावती","वर्धा","नागपूर","चंद्रपूर","अकोला","वाशीम","बुलढाणा","गडचिरोली","गोंदिया","भंडारा"), { district = it; saved = false })
        OutlinedTextField(area, { area = it; saved = false }, label = { Text("ऊस क्षेत्र (एकर)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
        DropdownField("जमिनीचा प्रकार", soil, listOf("काळी जमीन","मध्यम जमीन","हलकी / मुरमाड जमीन"), { soil = it; saved = false })
        DropdownField("पाण्याचा स्रोत", water, listOf("विहीर / बोअरवेल","ठिबक सिंचन","कालवा","पावसावर अवलंबून"), { water = it; saved = false })
        DropdownField("पिकाची अवस्था", stage, listOf("लागवडीपूर्व नियोजन","नुकतीच लागवड","वाढीची अवस्था","तोडणीपूर्व अवस्था"), { stage = it; saved = false })
        Button(onClick = {
            prefs.edit().putString("village", village.trim()).putString("district", district)
                .putString("area", area).putString("soil", soil).putString("water", water)
                .putString("stage", stage).apply()
            saved = true
            onSaved()
        }, modifier = Modifier.fillMaxWidth()) { Text("माहिती जतन करा") }
        if (saved) Text("माहिती या मोबाईलमध्ये जतन झाली.", color = green, fontWeight = FontWeight.Bold)
        Text("नोंदवलेली माहिती फोनच्या स्थानिक स्टोरेजमध्ये राहते. अ‍ॅप काढून टाकल्यास डेटा नष्ट होऊ शकतो.",
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DropdownField(label: String, selected: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selected, modifier = Modifier.weight(1f))
                Text("⌄")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
                }
            }
        }
    }
}

@Composable
private fun WaterScreen() {
    var moisture by remember { mutableStateOf("माहित नाही") }
    var rain by remember { mutableStateOf("नाही") }
    var standing by remember { mutableStateOf("नाही") }
    var result by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("पाणी व्यवस्थापन", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("खालील माहितीवरून प्राथमिक सूचना मिळवा. हा सेन्सर किंवा हवामान अंदाज नाही.")
        DropdownField("जमिनीतील ओलावा", moisture, listOf("माहित नाही","कोरडी","पुरेसा","खूप ओलसर"), { moisture = it; result = false })
        DropdownField("अलीकडे पाऊस झाला?", rain, listOf("नाही","हो"), { rain = it; result = false })
        DropdownField("शेतात पाणी साचले आहे?", standing, listOf("नाही","हो"), { standing = it; result = false })
        Button(onClick = { result = true }, modifier = Modifier.fillMaxWidth()) { Text("सल्ला पाहा") }
        if (result) {
            val title: String
            val body: String
            when {
                standing == "हो" -> {
                    title = "पाण्याचा निचरा तपासा"
                    body = "सध्या अतिरिक्त सिंचन टाळा. पाणी साचण्याचे कारण आणि निचरा तपासा."
                }
                moisture == "खूप ओलसर" -> {
                    title = "सिंचन थांबवा"
                    body = "जमिनीतील ओलावा व निचरा पुन्हा तपासून मग निर्णय घ्या."
                }
                rain == "हो" -> {
                    title = "पावसाचा परिणाम तपासा"
                    body = "मातीच्या प्रकारानुसार ओलावा तपासा. पुरेसा ओलावा असल्यास लगेच सिंचन करू नका."
                }
                moisture == "कोरडी" -> {
                    title = "सिंचनाची गरज तपासा"
                    body = "पिकाची अवस्था, माती, उपलब्ध पाणी आणि हवामान पाहून सिंचन ठरवा."
                }
                else -> {
                    title = "अधिक माहिती आवश्यक"
                    body = "जमिनीतील ओलावा प्रत्यक्ष तपासा. फक्त या माहितीतून सिंचनाची वेळ किंवा कालावधी ठरवता येत नाही."
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = paleGreen)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(title, fontWeight = FontWeight.Bold, color = green)
                    Text(body)
                    Text("सूचना: ही सामान्य माहिती आहे. पिकाची अवस्था व स्थानिक कृषी शिफारशींचा विचार करा.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ExpenseScreen() {
    var acres by remember { mutableStateOf("1") }
    var totalCost by remember { mutableStateOf("50000") }
    var tonnesPerAcre by remember { mutableStateOf("40") }
    var rate by remember { mutableStateOf("3000") }
    val areaValue = acres.toDoubleOrNull() ?: 0.0
    val costValue = totalCost.toDoubleOrNull() ?: 0.0
    val yieldValue = tonnesPerAcre.toDoubleOrNull() ?: 0.0
    val rateValue = rate.toDoubleOrNull() ?: 0.0
    val revenue = areaValue * yieldValue * rateValue
    val profit = revenue - costValue
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("खर्च व नफा कॅल्क्युलेटर", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("सर्व आकडे स्वतः भरा. उदाहरणार्थ दर व उत्पादन हे बाजारातील खात्रीशीर आकडे नाहीत.")
        NumberField("क्षेत्र (एकर)", acres) { acres = it }
        NumberField("एकूण खर्च (₹)", totalCost) { totalCost = it }
        NumberField("अपेक्षित उत्पादन (टन प्रति एकर)", tonnesPerAcre) { tonnesPerAcre = it }
        NumberField("अपेक्षित दर (₹ प्रति टन)", rate) { rate = it }
        Card(colors = CardDefaults.cardColors(containerColor = paleGreen)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("अंदाजित एकूण उत्पन्न", style = MaterialTheme.typography.titleSmall)
                Text("₹ ${money(revenue)}", style = MaterialTheme.typography.headlineSmall, color = green, fontWeight = FontWeight.Bold)
                Text("अंदाजित नफा / तोटा")
                Text("₹ ${money(profit)}", style = MaterialTheme.typography.titleLarge,
                    color = if (profit >= 0) green else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                Text("एकरी खर्च: ₹ ${money(if (areaValue > 0) costValue / areaValue else 0.0)}",
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text("हा साधा अंदाज आहे. तोडणी, वाहतूक, कपात, थकबाकी, भाडे आणि इतर खर्च एकूण खर्चात समाविष्ट करा.",
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun NumberField(label: String, value: String, onValue: (String) -> Unit) {
    OutlinedTextField(value, onValue, label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(), singleLine = true)
}

private fun money(value: Double): String =
    String.format(Locale("en", "IN"), "%,.0f", value)

@Composable
private fun NotesScreen(prefs: android.content.SharedPreferences, onSaved: () -> Unit) {
    var note by remember { mutableStateOf("") }
    var allNotes by remember { mutableStateOf(prefs.getString("notes", "") ?: "") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("शेत नोंदवही", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("आजचे निरीक्षण, काम किंवा पुढील आठवण लिहा.")
        OutlinedTextField(note, { note = it }, label = { Text("नोंद लिहा") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp))
        Button(onClick = {
            if (note.isNotBlank()) {
                val stamp = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale("mr", "IN")).format(Date())
                allNotes = "• $stamp — ${note.trim()}\n" + allNotes
                prefs.edit().putString("notes", allNotes).apply()
                note = ""
                onSaved()
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("नोंद जतन करा") }
        Divider()
        Text("जतन केलेल्या नोंदी", fontWeight = FontWeight.Bold)
        if (allNotes.isBlank()) Text("अजून कोणतीही नोंद नाही.")
        else Text(allNotes)
        OutlinedButton(onClick = {
            allNotes = ""
            prefs.edit().remove("notes").apply()
        }, modifier = Modifier.fillMaxWidth()) { Text("सर्व नोंदी पुसा") }
    }
}
