
package com.oosmitra.ai

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private val green = androidx.compose.ui.graphics.Color(0xFF176B3A)
private val paleGreen = androidx.compose.ui.graphics.Color(0xFFE7F4EA)

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
        val json = context.assets.open("maharashtra_locations.json")
            .bufferedReader(Charsets.UTF_8).use { it.readText() }

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
    HOME, FARM, WEATHER, WATER, EXPENSES, NOTES
}

@Composable
private fun OosMitraApp(context: Context) {
    val prefs = remember {
        context.getSharedPreferences("oosmitra_local", Context.MODE_PRIVATE)
    }
    var screen by remember { mutableStateOf(Screen.HOME) }

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
                    Column(
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Text(
                            "🌱 ऊस मित्र AI",
                            style = MaterialTheme.typography.headlineSmall,
                            color = androidx.compose.ui.graphics.Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "महाराष्ट्रातील शेतकऱ्यांसाठी मराठी सहाय्यक",
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color.White
                        )
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
                        selected = screen == Screen.WEATHER,
                        onClick = { screen = Screen.WEATHER },
                        icon = { Text("🌤") },
                        label = { Text("हवामान") }
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
            Column(Modifier.padding(padding).fillMaxSize()) {
                when (screen) {
                    Screen.HOME -> HomeScreen(
                        prefs = prefs,
                        onFarm = { screen = Screen.FARM },
                        onWeather = { screen = Screen.WEATHER },
                        onWater = { screen = Screen.WATER },
                        onExpenses = { screen = Screen.EXPENSES },
                        onNotes = { screen = Screen.NOTES }
                    )
                    Screen.FARM -> FarmScreen(prefs, context)
                    Screen.WEATHER -> WeatherScreen(prefs)
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
    onWeather: () -> Unit,
    onWater: () -> Unit,
    onExpenses: () -> Unit,
    onNotes: () -> Unit
) {
    val farmerName = prefs.getString("farmer_name", "")?.trim().orEmpty()
    val village = prefs.getString("village", "") ?: ""
    val district = prefs.getString("district", "") ?: ""

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = paleGreen)) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    if (farmerName.isBlank()) "नमस्कार शेतकरी मित्रा!"
                    else "नमस्कार, $farmerName!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = green
                )
                Text("तुमची शेतीची माहिती या मोबाईलमध्ये जतन होते.")
                Text(
                    when {
                        village.isNotBlank() && district.isNotBlank() -> "गाव: $village, जिल्हा: $district"
                        district.isNotBlank() -> "जिल्हा: $district"
                        else -> "तुमचा जिल्हा आणि गाव निवडा."
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Text("आजचे पर्याय", fontWeight = FontWeight.Bold)
        FeatureCard("🌾", "माझे शेत", "जिल्हा, तालुका, गाव व पीक माहिती", onFarm)
        FeatureCard("🌤️", "आजचे हवामान", "तुमच्या गावाचा थेट हवामान अंदाज", onWeather)
        FeatureCard("💧", "पाणी व्यवस्थापन", "जमिनीतील ओलाव्यानुसार प्राथमिक सूचना", onWater)
        FeatureCard("₹", "खर्च व नफा", "उत्पन्न आणि खर्चाचा अंदाज", onExpenses)
        FeatureCard("📝", "शेत नोंदवही", "शेतातील कामांची नोंद", onNotes)

        Text(
            "हवामानासाठी इंटरनेट आवश्यक आहे. हवामान माहिती: Open-Meteo.com.",
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
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
private fun FarmScreen(
    prefs: android.content.SharedPreferences,
    context: Context
) {
    val districts = remember(context) { loadLocations(context) }

    var farmerName by remember {
        mutableStateOf(prefs.getString("farmer_name", "") ?: "")
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
    var area by remember { mutableStateOf(prefs.getString("area", "1") ?: "1") }
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
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "माझ्या शेताची माहिती",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        if (districts.isEmpty()) {
            Card(colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "महाराष्ट्राची अधिकृत स्थानिक यादी लोड झाली नाही.",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "app/src/main/assets/maharashtra_locations.json ही डेटा फाइल जोडल्यानंतर Dropdown उपलब्ध होतील."
                    )
                }
            }
        }

        OutlinedTextField(
            value = farmerName,
            onValueChange = { farmerName = it; saved = false },
            label = { Text("शेतकऱ्याचे नाव") },
            placeholder = { Text("उदा. अक्षय") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

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
            onSelect = {
                taluka = it
                village = ""
                saved = false
            }
        )

        DropdownField(
            label = "गाव निवडा",
            selected = village.ifBlank { "गाव निवडा" },
            options = villageOptions,
            onSelect = {
                village = it
                saved = false
            }
        )

        OutlinedTextField(
            value = area,
            onValueChange = { area = it; saved = false },
            label = { Text("ऊस क्षेत्र (एकर)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        DropdownField(
            "जमिनीचा प्रकार", soil,
            listOf("काळी जमीन", "मध्यम जमीन", "हलकी / मुरमाड जमीन")
        ) { soil = it; saved = false }

        DropdownField(
            "पाण्याचा स्रोत", water,
            listOf("विहीर / बोअरवेल", "ठिबक सिंचन", "कालवा", "पावसावर अवलंबून")
        ) { water = it; saved = false }

        DropdownField(
            "पिकाची अवस्था", stage,
            listOf("लागवडीपूर्व नियोजन", "नुकतीच लागवड", "वाढीची अवस्था", "तोडणीपूर्व अवस्था")
        ) { stage = it; saved = false }

        Button(
            onClick = {
                prefs.edit()
                    .putString("farmer_name", farmerName.trim())
                    .putString("district", district)
                    .putString("taluka", taluka)
                    .putString("village", village)
                    .putString("area", area)
                    .putString("soil", soil)
                    .putString("water", water)
                    .putString("stage", stage)
                    .apply()
                saved = true
            },
            enabled = district.isNotBlank() &&
                taluka.isNotBlank() &&
                village.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("माहिती जतन करा")
        }

        if (saved) {
            Text(
                "नमस्कार${farmerName.trim().takeIf { it.isNotBlank() }?.let { ", $it" } ?: " शेतकरी मित्रा"}! तुमची माहिती मोबाईलमध्ये जतन झाली.",
                color = green,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            "नोंदवलेली माहिती फोनच्या स्थानिक स्टोरेजमध्ये राहते. अ‍ॅप काढून टाकल्यास डेटा नष्ट होऊ शकतो.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun DropdownField(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(
                onClick = { if (options.isNotEmpty()) expanded = true },
                enabled = options.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selected, modifier = Modifier.weight(1f))
                Text("⌄")
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 320.dp)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}


private data class WeatherInfo(
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val windKmh: Double,
    val weatherCode: Int,
    val maxTemp: Double,
    val minTemp: Double,
    val rainChance: Int,
    val updatedAt: String
)

private fun weatherDescription(code: Int): String = when (code) {
    0 -> "आकाश निरभ्र"
    1 -> "मुख्यतः निरभ्र"
    2 -> "अंशतः ढगाळ"
    3 -> "ढगाळ"
    45, 48 -> "धुके"
    51, 53, 55 -> "रिमझिम पाऊस"
    56, 57 -> "गोठणारा रिमझिम पाऊस"
    61, 63, 65 -> "पाऊस"
    66, 67 -> "गोठणारा पाऊस"
    71, 73, 75, 77 -> "हिमवृष्टी"
    80, 81, 82 -> "पावसाच्या सरी"
    85, 86 -> "हिमवृष्टीच्या सरी"
    95, 96, 99 -> "मेघगर्जनेसह वादळ"
    else -> "हवामानाची माहिती उपलब्ध"
}

private fun fetchWeatherForPlace(village: String, taluka: String, district: String): WeatherInfo {
    fun getJson(urlText: String): JSONObject {
        val connection = (URL(urlText).openConnection() as HttpURLConnection)
        connection.connectTimeout = 12000
        connection.readTimeout = 12000
        connection.requestMethod = "GET"
        try {
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("सर्व्हरकडून माहिती मिळाली नाही (${connection.responseCode})")
            }
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
        } finally {
            connection.disconnect()
        }
    }

    // Geocoding search works best with the village name; administrative names
    // are used below to choose the most plausible matching result.
    val encoded = URLEncoder.encode(village, "UTF-8")
    val geo = getJson("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=10&language=en&format=json")
    val results = geo.optJSONArray("results") ?: throw IllegalStateException("गाव सापडले नाही. माझ्या शेतात गावाची माहिती तपासा.")
    if (results.length() == 0) throw IllegalStateException("या गावाचे स्थान सापडले नाही. गावाचे नाव तपासा.")

    // Prefer matching district/taluka/village result where the geocoder provides administrative fields.
    var chosen = results.getJSONObject(0)
    for (i in 0 until results.length()) {
        val item = results.getJSONObject(i)
        val admin = (item.optString("admin1") + " " + item.optString("admin2") + " " + item.optString("name")).lowercase(Locale.ROOT)
        if (admin.contains(district.lowercase(Locale.ROOT)) || admin.contains(taluka.lowercase(Locale.ROOT))) {
            chosen = item
            break
        }
    }
    val lat = chosen.getDouble("latitude")
    val lon = chosen.getDouble("longitude")
    val forecastUrl = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
        "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m" +
        "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max,weather_code" +
        "&timezone=auto&forecast_days=1"
    val root = getJson(forecastUrl)
    val current = root.getJSONObject("current")
    val daily = root.getJSONObject("daily")
    return WeatherInfo(
        temperature = current.getDouble("temperature_2m"),
        feelsLike = current.getDouble("apparent_temperature"),
        humidity = current.getInt("relative_humidity_2m"),
        windKmh = current.getDouble("wind_speed_10m"),
        weatherCode = current.getInt("weather_code"),
        maxTemp = daily.getJSONArray("temperature_2m_max").getDouble(0),
        minTemp = daily.getJSONArray("temperature_2m_min").getDouble(0),
        rainChance = daily.getJSONArray("precipitation_probability_max").optInt(0, 0),
        updatedAt = current.optString("time", "")
    )
}

@Composable
private fun WeatherScreen(prefs: android.content.SharedPreferences) {
    val village = prefs.getString("village", "") ?: ""
    val taluka = prefs.getString("taluka", "") ?: ""
    val district = prefs.getString("district", "") ?: ""
    var weather by remember { mutableStateOf<WeatherInfo?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var lastLocation by remember { mutableStateOf("") }

    fun refresh() {
        if (village.isBlank() || district.isBlank()) {
            error = "कृपया प्रथम ‘माझे शेत’ मध्ये जिल्हा, तालुका आणि गाव निवडून माहिती जतन करा."
            return
        }
        loading = true
        error = ""
        Thread {
            try {
                val result = fetchWeatherForPlace(village, taluka, district)
                Handler(Looper.getMainLooper()).post {
                    weather = result
                    loading = false
                    lastLocation = "$village, $district"
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    error = e.message ?: "हवामान मिळवता आले नाही. इंटरनेट तपासा."
                    loading = false
                }
            }
        }.start()
    }

    LaunchedEffect(village, taluka, district) { refresh() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("आजचे हवामान", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = green)
        Text(if (village.isBlank()) "गाव निवडल्यानंतर स्थानिक हवामान दिसेल." else "स्थान: $village${if (taluka.isNotBlank()) ", $taluka" else ""}, $district")

        if (loading && weather == null) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("हवामानाची माहिती मिळवत आहोत…")
        }
        if (error.isNotBlank()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(error, Modifier.padding(12.dp))
            }
        }
        weather?.let { w ->
            Card(colors = CardDefaults.cardColors(containerColor = paleGreen)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(weatherDescription(w.weatherCode), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = green)
                    Text("%.1f°C".format(Locale.ROOT, w.temperature), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = green)
                    Text("जाणवणारे तापमान: %.1f°C".format(Locale.ROOT, w.feelsLike))
                    HorizontalDivider()
                    Text("कमाल तापमान: %.1f°C    |    किमान तापमान: %.1f°C".format(Locale.ROOT, w.maxTemp, w.minTemp))
                    Text("पावसाची शक्यता: ${w.rainChance}%")
                    Text("आर्द्रता: ${w.humidity}%")
                    Text("वाऱ्याचा वेग: %.1f किमी/तास".format(Locale.ROOT, w.windKmh))
                    if (w.updatedAt.isNotBlank()) Text("माहितीची वेळ: ${w.updatedAt.replace('T', ' ')}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("ऊस शेतीसाठी सूचना", fontWeight = FontWeight.Bold)
            Text(when {
                w.rainChance >= 70 -> "पावसाची शक्यता जास्त आहे. सिंचनाचा निर्णय घेण्यापूर्वी स्थानिक परिस्थिती आणि प्रत्यक्ष पाऊस तपासा."
                w.temperature >= 35 -> "उष्णता जास्त आहे. शेतातील ओलावा तपासा आणि सिंचनाबाबत स्थानिक कृषी सल्ला पाळा."
                w.weatherCode in listOf(61, 63, 65, 80, 81, 82, 95, 96, 99) -> "पाऊस किंवा वादळाची शक्यता/स्थिती आहे. शेतात पाणी साचणार नाही याची काळजी घ्या."
                else -> "हवामानानुसार निर्णय घेण्यापूर्वी जमिनीतील ओलावा, पिकाची अवस्था आणि स्थानिक कृषी सल्ला तपासा."
            })
        }
        Button(onClick = { refresh() }, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
            Text(if (loading) "माहिती मिळवत आहोत…" else "हवामान पुन्हा तपासा")
        }
        Text("हवामान डेटा: Open-Meteo.com · अंदाजात बदल होऊ शकतो. ही माहिती हमी किंवा अधिकृत आपत्ती सूचना नाही.", style = MaterialTheme.typography.bodySmall)
        Text("गावाचे नाव किंवा स्थान चुकीचे दिसल्यास ‘माझे शेत’ मध्ये गाव तपासा.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun WaterScreen() {
    var moisture by remember { mutableStateOf("माहित नाही") }
    var rain by remember { mutableStateOf("नाही") }
    var standing by remember { mutableStateOf("नाही") }
    var result by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("पाणी व्यवस्थापन", style = MaterialTheme.typography.titleLarge)
        Text("ही प्राथमिक सूचना आहे; सेन्सर किंवा हवामान अंदाज नाही.")

        DropdownField("जमिनीतील ओलावा", moisture,
            listOf("माहित नाही", "कोरडी", "पुरेसा", "खूप ओलसर")) {
            moisture = it
            result = false
        }
        DropdownField("अलीकडे पाऊस झाला?", rain, listOf("नाही", "हो")) {
            rain = it
            result = false
        }
        DropdownField("शेतात पाणी साचले आहे?", standing, listOf("नाही", "हो")) {
            standing = it
            result = false
        }

        Button(onClick = { result = true }, modifier = Modifier.fillMaxWidth()) {
            Text("सल्ला पाहा")
        }

        if (result) {
            val title: String
            val body: String

            when {
                standing == "हो" -> {
                    title = "पाण्याचा निचरा तपासा"
                    body = "अतिरिक्त सिंचन टाळा आणि निचरा तपासा."
                }
                moisture == "खूप ओलसर" -> {
                    title = "सिंचन थांबवा"
                    body = "ओलावा व निचरा पुन्हा तपासा."
                }
                rain == "हो" -> {
                    title = "पावसाचा परिणाम तपासा"
                    body = "मातीतील ओलावा तपासून सिंचनाचा निर्णय घ्या."
                }
                moisture == "कोरडी" -> {
                    title = "सिंचनाची गरज तपासा"
                    body = "पीक अवस्था, माती आणि उपलब्ध पाणी विचारात घ्या."
                }
                else -> {
                    title = "अधिक माहिती आवश्यक"
                    body = "जमिनीतील ओलावा प्रत्यक्ष तपासा."
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = paleGreen)) {
                Column(Modifier.padding(16.dp)) {
                    Text(title, fontWeight = FontWeight.Bold, color = green)
                    Text(body)
                    Text(
                        "स्थानिक कृषी शिफारशींचा विचार करा.",
                        style = MaterialTheme.typography.bodySmall
                    )
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

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("खर्च व नफा कॅल्क्युलेटर", style = MaterialTheme.typography.titleLarge)
        Text("उत्पादन व दर हे तुम्ही भरलेले अंदाज आहेत.")

        NumberField("क्षेत्र (एकर)", acres) { acres = it }
        NumberField("एकूण खर्च (₹)", totalCost) { totalCost = it }
        NumberField("उत्पादन (टन प्रति एकर)", tonnesPerAcre) { tonnesPerAcre = it }
        NumberField("दर (₹ प्रति टन)", rate) { rate = it }

        Card(colors = CardDefaults.cardColors(containerColor = paleGreen)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("अंदाजित एकूण उत्पन्न")
                Text(
                    "₹ ${money(revenue)}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = green,
                    fontWeight = FontWeight.Bold
                )
                Text("अंदाजित नफा / तोटा")
                Text(
                    "₹ ${money(profit)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (profit >= 0) green else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                Text("एकरी खर्च: ₹ ${money(if (areaValue > 0) costValue / areaValue else 0.0)}")
            }
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValue: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

private fun money(value: Double): String =
    String.format(Locale("en", "IN"), "%,.0f", value)

@Composable
private fun NotesScreen(
    prefs: android.content.SharedPreferences
) {
    var note by remember { mutableStateOf("") }
    var allNotes by remember {
        mutableStateOf(prefs.getString("notes", "") ?: "")
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("शेत नोंदवही", style = MaterialTheme.typography.titleLarge)
        Text("आजचे निरीक्षण, काम किंवा पुढील आठवण लिहा.")

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("नोंद लिहा") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp)
        )

        Button(
            onClick = {
                if (note.isNotBlank()) {
                    val stamp = SimpleDateFormat(
                        "dd-MM-yyyy HH:mm",
                        Locale("mr", "IN")
                    ).format(Date())

                    allNotes = "• $stamp — ${note.trim()}\n$allNotes"
                    prefs.edit().putString("notes", allNotes).apply()
                    note = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("नोंद जतन करा")
        }

        HorizontalDivider()
        Text("जतन केलेल्या नोंदी", fontWeight = FontWeight.Bold)

        if (allNotes.isBlank()) Text("अजून कोणतीही नोंद नाही.")
        else Text(allNotes)

        OutlinedButton(
            onClick = {
                allNotes = ""
                prefs.edit().remove("notes").apply()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("सर्व नोंदी पुसा")
        }
    }
}
