package com.oosmitra.ai

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val green = Color(0xFF176B3A)
private val paleGreen = Color(0xFFE7F4EA)

data class LocationDistrict(
    val name: String,
    val talukas: List<LocationTaluka>
)

data class LocationTaluka(
    val name: String,
    val villages: List<String>
)

private fun loadLocations(context: Context): List<LocationDistrict> {
    return try {
        val json = context.assets
            .open("maharashtra_locations.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }

        val root = JSONObject(json)
        val districtsJson = root.getJSONArray("districts")
        val districts = mutableListOf<LocationDistrict>()

        for (i in 0 until districtsJson.length()) {
            val districtObject = districtsJson.getJSONObject(i)
            val talukasJson = districtObject.getJSONArray("talukas")
            val talukas = mutableListOf<LocationTaluka>()

            for (j in 0 until talukasJson.length()) {
                val talukaObject = talukasJson.getJSONObject(j)
                val villagesJson = talukaObject.getJSONArray("villages")
                val villages = mutableListOf<String>()

                for (k in 0 until villagesJson.length()) {
                    villages.add(villagesJson.getString(k))
                }

                talukas.add(
                    LocationTaluka(
                        name = talukaObject.getString("name"),
                        villages = villages
                    )
                )
            }

            districts.add(
                LocationDistrict(
                    name = districtObject.getString("name"),
                    talukas = talukas
                )
            )
        }

        districts.sortedBy { it.name }
    } catch (e: Exception) {
        emptyList()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OosMitraApp(applicationContext)
        }
    }
}

private enum class Screen {
    HOME, FARM, WATER, EXPENSES, NOTES
}

@Composable
private fun OosMitraApp(context: Context) {
    val prefs = remember {
        context.getSharedPreferences(
            "oosmitra_local",
            Context.MODE_PRIVATE
        )
    }

    var screen by remember { mutableStateOf(Screen.HOME) }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = green,
            secondary = Color(0xFFB87913),
            background = Color(0xFFF7F8F5),
            surface = Color.White
        )
    ) {
        Scaffold(
            topBar = {
                Surface(color = green) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Logo file:
                        // app/src/main/res/drawable/oosmitra_logo.png
                        Image(
                            painter = painterResource(
                                id = R.drawable.oosmitra_logo
                            ),
                            contentDescription = "ऊस मित्र AI लोगो",
                            modifier = Modifier.size(58.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ऊस मित्र AI",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "नमस्कार शेतकरी मित्रा!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )

                            Text(
                                text = "महाराष्ट्रातील शेतकऱ्यांसाठी मराठी सहाय्यक",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = screen == Screen.HOME,
                        onClick = { screen = Screen.HOME },
                        icon = { Text("⌂") },
                        label = { Text("मुख्य") }
                    )

                    NavigationBarItem(
                        selected = screen == Screen.FARM,
                        onClick = { screen = Screen.FARM },
                        icon = { Text("🌾") },
                        label = { Text("माझे शेत") }
                    )

                    NavigationBarItem(
                        selected = screen == Screen.WATER,
                        onClick = { screen = Screen.WATER },
                        icon = { Text("💧") },
                        label = { Text("पाणी") }
                    )

                    NavigationBarItem(
                        selected = screen == Screen.EXPENSES,
                        onClick = { screen = Screen.EXPENSES },
                        icon = { Text("₹") },
                        label = { Text("खर्च") }
                    )

                    NavigationBarItem(
                        selected = screen == Screen.NOTES,
                        onClick = { screen = Screen.NOTES },
                        icon = { Text("☷") },
                        label = { Text("नोंदी") }
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                when (screen) {
                    Screen.HOME -> HomeScreen(
                        prefs = prefs,
                        onFarm = { screen = Screen.FARM },
                        onWater = { screen = Screen.WATER },
                        onExpenses = { screen = Screen.EXPENSES },
                        onNotes = { screen = Screen.NOTES }
                    )

                    Screen.FARM -> FarmScreen(prefs, context)
                    Screen.WATER -> WaterScreen()
                    Screen.EXPENSES -> ExpenseScreen()
                    Screen.NOTES -> NotesScreen(prefs)
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    prefs: android.content.SharedPreferences,
    onFarm: () -> Unit,
    onWater: () -> Unit,
    onExpenses: () -> Unit,
    onNotes: () -> Unit
) {
    val village = prefs.getString("village", "") ?: ""
    val district = prefs.getString("district", "") ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = paleGreen
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (village.isBlank()) {
                        "नमस्कार शेतकरी मित्रा!"
                    } else {
                        "नमस्कार! $village"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = green
                )

                Text("तुमची शेतीची माहिती या मोबाईलमध्ये जतन होते.")

                Text(
                    text = if (district.isBlank()) {
                        "तुमचा जिल्हा आणि गाव निवडा."
                    } else {
                        "जिल्हा: $district"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Text("आजचे पर्याय", fontWeight = FontWeight.Bold)

        FeatureCard(
            "🌾", "माझे शेत",
            "जिल्हा, तालुका, गाव व पीक माहिती", onFarm
        )

        FeatureCard(
            "💧", "पाणी व्यवस्थापन",
            "जमिनीतील ओलाव्यानुसार प्राथमिक सूचना", onWater
        )

        FeatureCard(
            "₹", "खर्च व नफा",
            "उत्पन्न आणि खर्चाचा अंदाज", onExpenses
        )

        FeatureCard(
            "📝", "शेत नोंदवही",
            "शेतातील कामांची नोंद", onNotes
        )

        Text(
            "टीप: हा प्राथमिक ऑफलाइन नमुना आहे. तो थेट हवामान डेटा किंवा रोग ओळखणारे प्रशिक्षित AI वापरत नाही.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun FeatureCard(
    icon: String,
    title: String,
    detail: String,
    onClick: () -> Unit
) {
    Card(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(icon, style = MaterialTheme.typography.headlineMedium)

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(detail, style = MaterialTheme.typography.bodySmall)
            }

            Text(
                "›",
                style = MaterialTheme.typography.headlineMedium,
                color = green
            )
        }
    }
}

@Composable
private fun FarmScreen(
    prefs: android.content.SharedPreferences,
    context: Context
) {
    val districts = remember(context) {
        loadLocations(context)
    }

    var district by remember {
        mutableStateOf(prefs.getString("district", "") ?: "")
    }

    var taluka by remember {
        mutableStateOf(prefs.getString("taluka", "") ?: "")
    }

    var village by remember {
        mutableStateOf(prefs.getString("village", "") ?: "")
    }

    var area by remember {
        mutableStateOf(prefs.getString("area", "1") ?: "1")
    }

    var soil by remember {
        mutableStateOf(prefs.getString("soil", "काळी जमीन") ?: "काळी जमीन")
    }

    var water by remember {
        mutableStateOf(prefs.getString("water", "विहीर / बोअरवेल") ?: "विहीर / बोअरवेल")
    }

    var stage by remember {
        mutableStateOf(prefs.getString("stage", "वाढीची अवस्था") ?: "वाढीची अवस्था")
    }

    var saved by remember { mutableStateOf(false) }

    val selectedDistrict = districts.find { it.name == district }
    val talukaOptions = selectedDistrict?.talukas ?: emptyList()
    val selectedTaluka = talukaOptions.find { it.name == taluka }
    val villageOptions = selectedTaluka?.villages ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "माझ्या शेताची माहिती",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        if (districts.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "स्थानिक यादी लोड झाली नाही.",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "app/src/main/assets/maharashtra_locations.json ही फाइल तपासा."
                    )
                }
            }
        }

        DropdownField(
            label = "जिल्हा निवडा",
            selected = district.ifBlank { "जिल्हा निवडा" },
            options = districts.map { it.name },
            onSelect = {
                district = it
                taluka = ""
                village = ""
                saved = false
            }
        )

        DropdownField(
            label = "तालुका निवडा",
            selected = taluka.ifBlank { "तालुका निवडा" },
            options = talukaOptions.map { it.name },
            on
